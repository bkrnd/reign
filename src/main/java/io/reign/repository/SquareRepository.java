package io.reign.repository;

import io.reign.model.Square;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SquareRepository extends JpaRepository<Square, String> {

    List<Square> findByWorldSlug(String worldSlug);

    Optional<Square> findByWorldSlugAndXAndY(String worldSlug, int x, int y);

    void deleteByWorldSlug(String worldSlug);

    @Modifying
    @Query("UPDATE Square s SET s.ownerId = NULL, s.defenseBonus = 0 WHERE s.worldSlug = :worldSlug")
    int resetAllSquares(@Param("worldSlug") String worldSlug);
}