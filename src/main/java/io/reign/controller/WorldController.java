package io.reign.controller;

import io.reign.dto.ErrorResponse;
import io.reign.enums.BoardType;
import io.reign.model.Square;
import io.reign.model.User;
import io.reign.model.World;
import io.reign.repository.WorldRepository;
import io.reign.service.WorldService;
import jakarta.persistence.Column;
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
    public ResponseEntity<?> createWorld(
            @RequestBody CreateWorldRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        // Check if user is authenticated
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("User not authenticated"));
        }

        // Check if slug already exists
        if (worldRepository.existsBySlug(request.getSlug())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("World with slug '" + request.getSlug() + "' already exists"));
        }

        try {
            // Use authenticated user as owner (ignore ownerId from request)
            World world = worldService.createWorld(
                    request.getSlug(),
                    request.getName(),
                    authenticatedUser.getId(),
                    request.getBoardType(),
                    request.getBoardSize(),
                    request.getMaxActionPoints(),
                    request.getCycleDurationInSeconds(),
                    request.getActionPointsPerCycle(),
                    request.getMaxPlayers(),
                    request.getMaxTeams(),
                    request.getMinTeams(),
                    request.getMaxTeamSize(),
                    request.getMinTeamSize(),
                    request.getAllowPlayerTeamCreation(),
                    request.getIsPublic()
            );

            return ResponseEntity.ok(world);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("An error occurred while creating the world"));
        }
    }

    @GetMapping
    public List<World> getAllWorlds(
            @AuthenticationPrincipal User authenticatedUser,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) BoardType boardType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean hideFull
    ) {
        // If any filter is provided, use the filtered query
        if (isPublic != null || boardType != null || search != null || hideFull != null) {
            String userId = authenticatedUser != null ? authenticatedUser.getId() : null;
            return worldService.getWorldsWithFilters(userId, isPublic, boardType, search, hideFull);
        }

        // Otherwise use the original logic
        if (authenticatedUser == null) {
            return worldService.getPublicWorlds();
        }
        return worldService.getWorldsForUser(authenticatedUser.getId());
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
    public ResponseEntity<?> updateWorld(
            @PathVariable String slug,
            @RequestBody CreateWorldRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        // Check if world exists
        World world = worldService.getWorldBySlug(slug).orElse(null);
        if (world == null) {
            return ResponseEntity.status(404).body(new ErrorResponse("World not found"));
        }

        // Check if authenticated user is the owner
        if (authenticatedUser == null || !world.getOwner().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(403).body(new ErrorResponse("You do not have permission to update this world"));
        }

        try {
            // Don't allow ownership changes (pass null for ownerId)
            World updated = worldService.updateWorld(
                    slug,
                    request.getName(),
                    null,
                    request.getBoardType(),
                    request.getBoardSize(),
                    request.getMaxActionPoints(),
                    request.getCycleDurationInSeconds(),
                    request.getActionPointsPerCycle(),
                    request.getMaxPlayers(),
                    request.getMaxTeams(),
                    request.getMinTeams(),
                    request.getMaxTeamSize(),
                    request.getMinTeamSize(),
                    request.getAllowPlayerTeamCreation(),
                    request.getIsPublic()
            );
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("An error occurred while updating the world"));
        }
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<?> deleteWorld(
            @PathVariable String slug,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        // Check if world exists
        World world = worldService.getWorldBySlug(slug).orElse(null);
        if (world == null) {
            return ResponseEntity.status(404).body(new ErrorResponse("World not found"));
        }

        // Check if authenticated user is the owner
        if (authenticatedUser == null || !world.getOwner().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(403).body(new ErrorResponse("You do not have permission to delete this world"));
        }

        try {
            worldService.deleteWorld(slug);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("An error occurred while deleting the world"));
        }
    }

    @PostMapping("/{slug}/reset")
    public ResponseEntity<?> resetBoard(
            @PathVariable String slug,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        // Check if world exists
        World world = worldService.getWorldBySlug(slug).orElse(null);
        if (world == null) {
            return ResponseEntity.status(404).body(new ErrorResponse("World not found"));
        }

        // Check if authenticated user is the owner
        if (authenticatedUser == null || !world.getOwner().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(403).body(new ErrorResponse("You do not have permission to reset this world"));
        }

        try {
            World resetWorld = worldService.resetWorldBoard(slug, authenticatedUser.getId());
            return ResponseEntity.ok(resetWorld);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("An error occurred while resetting the world"));
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
    private Integer maxActionPoints;
    private Integer cycleDurationInSeconds;
    private Integer actionPointsPerCycle;
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

    public Integer getMaxActionPoints() { return maxActionPoints; }
    public void setMaxActionPoints(Integer maxActionPoints) { this.maxActionPoints = maxActionPoints; }

    public Integer getCycleDurationInSeconds() { return cycleDurationInSeconds; }
    public void setCycleDurationInSeconds(Integer cycleDurationInSeconds) { this.cycleDurationInSeconds = cycleDurationInSeconds; }

    public Integer getActionPointsPerCycle() { return actionPointsPerCycle; }
    public void setActionPointsPerCycle(Integer actionPointsPerCycle) { this.actionPointsPerCycle = actionPointsPerCycle; }

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