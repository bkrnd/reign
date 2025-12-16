package io.reign.service;

import io.reign.model.Square;
import io.reign.model.SquareUpdateMessage;
import io.reign.model.User;
import io.reign.model.World;
import io.reign.repository.SquareRepository;
import io.reign.repository.UserRepository;
import io.reign.repository.WorldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class GameService {

    @Autowired
    private SquareRepository squareRepository;

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PreAuthorize("hasPermission(#worldSlug, 'WORLD_MEMBER')")
    @Transactional
    public Square captureSquare(String worldSlug, int x, int y, String playerId) {
        // Fetch world
        World world = worldRepository.findBySlug(worldSlug)
                .orElseThrow(() -> new IllegalArgumentException("World not found"));

        // Fetch player
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        // Find the square
        Square square = squareRepository.findByWorldAndXAndY(world, x, y)
                .orElseThrow(() -> new IllegalArgumentException("Square not found"));

        // Check if square is unowned
        if (square.getOwner() == null) {
            square.setOwner(player);
            Square updated = squareRepository.save(square);
            broadcastAfterCommit(worldSlug, "SQUARE_CAPTURED", updated, playerId);
            return updated;
        }

        // Prevent capturing own square
        if (square.getOwner().getId().equals(playerId)) {
            throw new IllegalArgumentException("Square is already owned by the player");
        }

        if (square.getDefenseBonus() > 0) {
            // Reduce defense bonus instead of capturing
            square.setDefenseBonus(square.getDefenseBonus() - 1);
        } else {
            // Capture the square
            square.setOwner(player);
        }

        Square updated = squareRepository.save(square);
        broadcastAfterCommit(worldSlug, "SQUARE_CAPTURED", updated, playerId);
        return updated;
    }

    @PreAuthorize("hasPermission(#worldSlug, 'WORLD_MEMBER')")
    @Transactional
    public Square defendSquare(String worldSlug, int x, int y, String playerId) {
        // Fetch world
        World world = worldRepository.findBySlug(worldSlug)
                .orElseThrow(() -> new IllegalArgumentException("World not found"));

        // Find the square
        Square square = squareRepository.findByWorldAndXAndY(world, x, y)
                .orElseThrow(() -> new IllegalArgumentException("Square not found"));

        // Check if square is owned by the player
        if (square.getOwner() == null || !playerId.equals(square.getOwner().getId())) {
            throw new IllegalArgumentException("Square is not owned by the player");
        }

        // Defend own square
        square.setDefenseBonus(1);
        Square updated = squareRepository.save(square);
        broadcastAfterCommit(worldSlug, "SQUARE_DEFENDED", updated, playerId);
        return updated;
    }

    private void broadcastAfterCommit(String worldSlug, String messageType, Square square, String playerId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Fetch world and entire board state
                World world = worldRepository.findBySlug(worldSlug)
                        .orElseThrow(() -> new IllegalArgumentException("World not found"));
                List<Square> board = squareRepository.findByWorld(world);

                SquareUpdateMessage message = new SquareUpdateMessage(
                    messageType,
                    board,
                    playerId,
                    System.currentTimeMillis()
                );
                messagingTemplate.convertAndSend("/topic/worlds/" + worldSlug, message);
            }
        });
    }
}