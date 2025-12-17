package io.reign.repository;

import io.reign.model.Square;
import io.reign.model.Team;
import io.reign.model.World;
import io.reign.model.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SquareRepository extends JpaRepository<Square, String> {

    List<Square> findByWorld(World world);

    Optional<Square> findByWorldAndXAndY(World world, int x, int y);

    void deleteByWorld(World world);

    @Modifying
    @Query("UPDATE Square s SET s.owner = NULL, s.defenseBonus = 0 WHERE s.world = :world")
    int resetAllSquares(@Param("world") World world);

    @Modifying
    @Query("UPDATE Square s SET s.owner = NULL, s.defenseBonus = 0 WHERE s.owner.id = :userId AND s.world = :world")
    int resetSquaresByUserAndWorld(@Param("userId") String userId, @Param("world") World world);

    @Modifying
    @Query("UPDATE Square s SET s.owner = NULL, s.defenseBonus = 0 WHERE s.owner.id = :userId")
    int resetSquaresByUser(@Param("userId") String userId);

    // Count squares in a world where the square's owner is a member of the provided team.
    // User does not have a direct `team` field, so we check TeamMember relations.
    @Query("SELECT COUNT(s) FROM Square s, TeamMember tm WHERE tm.user = s.owner AND s.world = :world AND tm.team = :team")
    long countByWorldAndOwnerTeam(World world, Team team);
}