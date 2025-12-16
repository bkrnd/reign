package io.reign.repository;

import io.reign.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u JOIN u.teamMemberships tm WHERE tm.team.id = :teamId")
    List<User> findByTeamId(@Param("teamId") String teamId);

    @Query("SELECT u FROM User u JOIN u.teamMemberships tm WHERE tm.team.world.id = :worldId")
    List<User> findByWorldId(@Param("worldId") String worldId);
}