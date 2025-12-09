package io.reign.controller;

import io.reign.model.World;
import io.reign.repository.WorldRepository;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/worlds")
public class WorldController {

    @Autowired
    private WorldRepository worldRepository;

    @PostMapping
    public ResponseEntity<World> createWorld(@RequestBody CreateWorldRequest request) {
        // Check if slug already exists
        if (worldRepository.existsBySlug(request.getSlug())) {
            return ResponseEntity.badRequest().build();
        }

        World world = new World();
        world.setSlug(request.getSlug());
        world.setName(request.getName());
        world.setOwnerId(request.getOwnerId());
        world.setBoardSize(request.getBoardSize() != null ? request.getBoardSize() : 20);
        world.setMaxPlayers(request.getMaxPlayers() != null ? request.getMaxPlayers() : 50);

        World saved = worldRepository.save(world);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public List<World> getAllWorlds() {
        return worldRepository.findAll();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<World> getWorldBySlug(@PathVariable String slug) {
        return worldRepository.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{slug}")
    public ResponseEntity<World> updateWorld(@PathVariable String slug, @RequestBody CreateWorldRequest request) {
        return worldRepository.findBySlug(slug)
                .map(world -> {
                    world.setName(request.getName());
                    world.setBoardSize(request.getBoardSize() != null ? request.getBoardSize() : world.getBoardSize());
                    world.setMaxPlayers(request.getMaxPlayers() != null ? request.getMaxPlayers() : world.getMaxPlayers());
                    World updated = worldRepository.save(world);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteWorld(@PathVariable String slug) {
        return worldRepository.findBySlug(slug)
                .map(world -> {
                    worldRepository.delete(world);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

// Simple DTO for create request
class CreateWorldRequest {
    private String slug;
    private String name;
    private String ownerId;
    private Integer boardSize;
    private Integer maxPlayers;

    // Getters and setters (or use @Data if Lombok)
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
}