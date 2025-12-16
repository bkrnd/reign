package io.reign.repository;

import io.reign.model.World;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorldRepository extends JpaRepository<World, String> {

    Optional<World> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT DISTINCT w FROM World w LEFT JOIN FETCH w.teams t LEFT JOIN FETCH t.members")
    List<World> findAllWithTeamsAndMembers();

    @Query("SELECT DISTINCT w FROM World w LEFT JOIN FETCH w.teams t LEFT JOIN FETCH t.members WHERE w.id = :id")
    Optional<World> findByIdWithTeamsAndMembers(@Param("id") String id);

    @Query("SELECT DISTINCT w FROM World w LEFT JOIN FETCH w.teams t LEFT JOIN FETCH t.members WHERE w.slug = :slug")
    Optional<World> findBySlugWithTeamsAndMembers(@Param("slug") String slug);

    List<World> findByIsPublicTrue();
}