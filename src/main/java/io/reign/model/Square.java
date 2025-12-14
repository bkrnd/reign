package io.reign.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "squares",
        uniqueConstraints = @UniqueConstraint(columnNames = {"world_id", "x", "y"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Square {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "world_id", nullable = false)
    private World world;

    @Column(nullable = false)
    private int x;

    @Column(nullable = false)
    private int y;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = true)
    private User owner;

    private int defenseBonus = 0;

    @JsonProperty("ownerId")
    public String getOwnerId() {
        return owner != null ? owner.getId() : null;
    }
}