package io.reign.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "worlds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class World {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String slug;  // URL-friendly: "acme-engineering"

    @Column(nullable = false)
    private String name;  // Display name: "Acme Engineering Team"

    private String ownerId;  // Who created this world

    private int boardSize = 20;  // Default 20x20 grid

    private int maxPlayers = 50;  // Default max players

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}