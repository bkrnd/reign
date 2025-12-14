package io.reign.repository;

import io.reign.model.Square;
import io.reign.model.World;
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
}