package io.reign.service;

import io.reign.model.Square;
import io.reign.repository.SquareRepository;
import io.reign.repository.WorldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    @Autowired
    private SquareRepository squareRepository;

    @Autowired
    private WorldRepository worldRepository;

    @Transactional
    public Square captureSquare(String worldSlug, int x, int y, String playerId) {
        // Check if world exists
        if (!worldRepository.existsBySlug(worldSlug)) {
            throw new IllegalArgumentException("World not found");
        }

        // Find the square
        Square square = squareRepository.findByWorldSlugAndXAndY(worldSlug, x, y)
                .orElseThrow(() -> new IllegalArgumentException("Square not found"));

        // Check if square is unowned (null or empty)
        if (square.getOwnerId() == null || square.getOwnerId().isEmpty()) {
            // Capture unowned square directly
            square.setOwnerId(playerId);
            return squareRepository.save(square);
        }

        // Prevent capturing own square
        if (square.getOwnerId().equals(playerId)) {
            throw new IllegalArgumentException("Square is already owned by the player");
        }

        if (square.getDefenseBonus() > 0) {
            // Reduce defense bonus instead of capturing
            square.setDefenseBonus(square.getDefenseBonus() - 1);
        } else {
            // Capture the square
            square.setOwnerId(playerId);
        }

        return squareRepository.save(square);
    }

    @Transactional
    public Square defendSquare(String worldSlug, int x, int y, String playerId) {
        // Check if world exists
        if (!worldRepository.existsBySlug(worldSlug)) {
            throw new IllegalArgumentException("World not found");
        }

        // Find the square
        Square square = squareRepository.findByWorldSlugAndXAndY(worldSlug, x, y)
                .orElseThrow(() -> new IllegalArgumentException("Square not found"));

        // Check if square is owned by the player
        if (!playerId.equals(square.getOwnerId())) {
            throw new IllegalArgumentException("Square is not owned by the player");
        }

        // Defend own square
        square.setDefenseBonus(1);
        return squareRepository.save(square);
    }
}