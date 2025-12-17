package io.reign.controller;

import io.reign.enums.BoardType;
import io.reign.model.Square;
import io.reign.model.User;
import io.reign.model.World;
import io.reign.repository.WorldRepository;
import io.reign.service.WorldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/worlds")
public class WorldController {

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private WorldService worldService;

    @PostMapping
    public ResponseEntity<World> createWorld(
            @RequestBody CreateWorldRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        // Check if user is authenticated
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build();
        }

        // Check if slug already exists
        if (worldRepository.existsBySlug(request.getSlug())) {
            return ResponseEntity.badRequest().build();
        }

        // Use authenticated user as owner (ignore ownerId from request)
        World world = worldService.createWorld(
                request.getSlug(),
                request.getName(),
                authenticatedUser.getId(),
                request.getBoardType(),
                request.getBoardSize(),
                request.getMaxPlayers(),
                request.getMaxTeams(),
                request.getMinTeams(),
                request.getMaxTeamSize(),
                request.getMinTeamSize(),
                request.getAllowPlayerTeamCreation(),
                request.getIsPublic()
        );

        return ResponseEntity.ok(world);
    }

    @GetMapping
    public List<World> getAllWorlds() {
        return worldService.getPublicWorlds();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<World> getWorldBySlug(@PathVariable String slug) {
        return worldService.getWorldBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasPermission(#slug, 'WORLD_MEMBER')")
    @GetMapping("/{slug}/board")
    public ResponseEntity<List<Square>> getWorldBoard(@PathVariable String slug) {
        // Check if world exists
        if (worldService.getWorldBySlug(slug).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Square> board = worldService.getWorldBoard(slug);
        return ResponseEntity.ok(board);
    }

    @PutMapping("/{slug}")
    public ResponseEntity<World> updateWorld(
            @PathVariable String slug,
            @RequestBody CreateWorldRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        // Check if world exists
        World world = worldService.getWorldBySlug(slug).orElse(null);
        if (world == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if authenticated user is the owner
        if (authenticatedUser == null || !world.getOwner().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(403).build();
        }

        try {
            // Don't allow ownership changes (pass null for ownerId)
            World updated = worldService.updateWorld(
                    slug,
                    request.getName(),
                    null,
                    request.getBoardType(),
                    request.getBoardSize(),
                    request.getMaxPlayers(),
                    request.getMaxTeams(),
                    request.getMinTeams(),
                    request.getMaxTeamSize(),
                    request.getMinTeamSize(),
                    request.getAllowPlayerTeamCreation(),
                    request.getIsPublic()
            );
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteWorld(
            @PathVariable String slug,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        // Check if world exists
        World world = worldService.getWorldBySlug(slug).orElse(null);
        if (world == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if authenticated user is the owner
        if (authenticatedUser == null || !world.getOwner().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(403).build();
        }

        worldService.deleteWorld(slug);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{slug}/reset")
    public ResponseEntity<World> resetBoard(
            @PathVariable String slug,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        // Check if world exists
        World world = worldService.getWorldBySlug(slug).orElse(null);
        if (world == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if authenticated user is the owner
        if (authenticatedUser == null || !world.getOwner().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(403).build();
        }

        try {
            World resetWorld = worldService.resetWorldBoard(slug, authenticatedUser.getId());
            return ResponseEntity.ok(resetWorld);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

// DTO
class CreateWorldRequest {
    private String slug;
    private String name;
    private String ownerId;
    private BoardType boardType;
    private Integer boardSize;
    private Integer maxPlayers;
    private Integer maxTeams;
    private Integer minTeams;
    private Integer maxTeamSize;
    private Integer minTeamSize;
    private Boolean allowPlayerTeamCreation;
    private Boolean isPublic;

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public BoardType getBoardType() { return boardType; }
    public void setBoardType(BoardType boardType) { this.boardType = boardType; }

    public Integer getBoardSize() { return boardSize; }
    public void setBoardSize(Integer boardSize) { this.boardSize = boardSize; }

    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }

    public Integer getMaxTeams() { return maxTeams; }
    public void setMaxTeams(Integer maxTeams) { this.maxTeams = maxTeams; }

    public Integer getMinTeams() { return minTeams; }
    public void setMinTeams(Integer minTeams) { this.minTeams = minTeams; }

    public Integer getMaxTeamSize() { return maxTeamSize; }
    public void setMaxTeamSize(Integer maxTeamSize) { this.maxTeamSize = maxTeamSize; }

    public Integer getMinTeamSize() { return minTeamSize; }
    public void setMinTeamSize(Integer minTeamSize) { this.minTeamSize = minTeamSize; }

    public Boolean getAllowPlayerTeamCreation() { return allowPlayerTeamCreation; }
    public void setAllowPlayerTeamCreation(Boolean allowPlayerTeamCreation) { this.allowPlayerTeamCreation = allowPlayerTeamCreation; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}