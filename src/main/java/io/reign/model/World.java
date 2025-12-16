package io.reign.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "worlds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"squares", "teams"})
public class World {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @JsonIgnore
    @OneToMany(mappedBy = "world", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Square> squares;

    @OneToMany(mappedBy = "world", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Team> teams;

    @Column(nullable = false)
    private boolean isPublic = true;

    @Column(nullable = false)
    private int boardSize = 25;

    @Column(nullable = false)
    private int maxPlayers = 6;

    @Column(nullable = false)
    private int maxTeams = 6;

    @Column(nullable = false)
    private int minTeams = 2;

    @Column(nullable = false)
    private int maxTeamSize = 3;

    @Column(nullable = false)
    private int minTeamSize = 1;

    @Column(nullable = false)
    private boolean allowPlayerTeamCreation = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}