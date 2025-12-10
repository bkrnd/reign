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

        // Check if square is empty
        if (square.getOwnerId() != null) {
            throw new IllegalArgumentException("Square is already owned");
        }

        // Capture the square
        square.setOwnerId(playerId);
        return squareRepository.save(square);
    }
}