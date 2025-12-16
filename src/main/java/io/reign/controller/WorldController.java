package io.reign.controller;

import io.reign.model.Square;
import io.reign.model.World;
import io.reign.repository.WorldRepository;
import io.reign.service.WorldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<World> createWorld(@RequestBody CreateWorldRequest request) {
        // Check if slug already exists
        if (worldRepository.existsBySlug(request.getSlug())) {
            return ResponseEntity.badRequest().build();
        }

        World world = worldService.createWorld(
                request.getSlug(),
                request.getName(),
                request.getOwnerId(),
                request.getBoardSize(),
                request.getMaxPlayers(),
                request.getMaxTeams(),
                request.getMinTeams(),
                request.getMaxTeamSize(),
                request.getMinTeamSize(),
                request.getAllowPlayerTeamCreation()
        );

        return ResponseEntity.ok(world);
    }

    @GetMapping
    public List<World> getAllWorlds() {
        return worldService.getAllWorlds();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<World> getWorldBySlug(@PathVariable String slug) {
        return worldService.getWorldBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

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
    public ResponseEntity<World> updateWorld(@PathVariable String slug, @RequestBody CreateWorldRequest request) {
        try {
            World updated = worldService.updateWorld(
                    slug,
                    request.getName(),
                    request.getOwnerId(),
                    request.getBoardSize(),
                    request.getMaxPlayers(),
                    request.getMaxTeams(),
                    request.getMinTeams(),
                    request.getMaxTeamSize(),
                    request.getMinTeamSize(),
                    request.getAllowPlayerTeamCreation()
            );
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteWorld(@PathVariable String slug) {
        if (worldService.getWorldBySlug(slug).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        worldService.deleteWorld(slug);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{slug}/reset")
    public ResponseEntity<World> resetBoard(
            @PathVariable String slug,
            @RequestBody(required = false) ResetRequest request
    ) {
        try {
            String playerId = request != null ? request.getPlayerId() : null;
            World world = worldService.resetWorldBoard(slug, playerId);
            return ResponseEntity.ok(world);
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
    private Integer boardSize;
    private Integer maxPlayers;
    private Integer maxTeams;
    private Integer minTeams;
    private Integer maxTeamSize;
    private Integer minTeamSize;
    private Boolean allowPlayerTeamCreation;

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

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
}

class ResetRequest {
    private String playerId;

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
}