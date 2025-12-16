package io.reign.repository;

import io.reign.model.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, String> {

    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId AND tm.team.world.id = :worldId")
    Optional<TeamMember> findByUserIdAndWorldId(@Param("userId") String userId, @Param("worldId") String worldId);

    void deleteByUserIdAndTeamId(String userId, String teamId);

    int countByTeamId(String teamId);

    boolean existsByUserIdAndTeamId(String userId, String teamId);
}