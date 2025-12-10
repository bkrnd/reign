package io.reign.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "squares",
        uniqueConstraints = @UniqueConstraint(columnNames = {"world_slug", "x", "y"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Square {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "world_slug", nullable = false)
    private String worldSlug;

    @Column(nullable = false)
    private int x;

    @Column(nullable = false)
    private int y;

    private String ownerId;  // NULL = empty square

    private int defenseBonus = 0;
}