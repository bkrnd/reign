package io.reign.repository;

import io.reign.model.World;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorldRepository extends JpaRepository<World, String> {

    Optional<World> findBySlug(String slug);

    boolean existsBySlug(String slug);
}