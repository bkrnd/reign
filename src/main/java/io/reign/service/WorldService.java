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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WorldService {

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SquareRepository squareRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public World createWorld(String slug, String name, String ownerId, Integer boardSize, Integer maxPlayers) {
        World world = new World();
        world.setSlug(slug);
        world.setName(name);
        if (ownerId != null) {
            User owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            world.setOwner(owner);
        }
        world.setBoardSize(boardSize != null ? boardSize : 20);
        world.setMaxPlayers(maxPlayers != null ? maxPlayers : 50);

        World saved = worldRepository.save(world);

        initializeBoard(saved, saved.getBoardSize());

        return saved;
    }

    @Transactional
    public World updateWorld(String slug, String name, String ownerId, Integer boardSize, Integer maxPlayers) {
        World world = worldRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("World not found"));

        int oldBoardSize = world.getBoardSize();

        if (name != null) world.setName(name);
        if (ownerId != null) {
            User owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            world.setOwner(owner);
        }
        if (boardSize != null) world.setBoardSize(boardSize);
        if (maxPlayers != null) world.setMaxPlayers(maxPlayers);

        World updated = worldRepository.save(world);

        // Reset board if size changed
        if (boardSize != null && boardSize != oldBoardSize) {
            resetBoard(updated, boardSize);
        }

        return updated;
    }

    @Transactional
    public void deleteWorld(String slug) {
        World world = worldRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("World not found"));

        squareRepository.deleteByWorld(world);
        worldRepository.delete(world);
    }

    @Transactional
    public World resetWorldBoard(String slug, String playerId) {
        World world = worldRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("World not found"));

        // Use bulk update query
        squareRepository.resetAllSquares(world);

        // Broadcast ONLY AFTER transaction commits
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Fetch entire board state
                List<Square> board = squareRepository.findByWorld(world);

                SquareUpdateMessage message = new SquareUpdateMessage(
                    "WORLD_RESET",
                    board,
                    playerId,
                    System.currentTimeMillis()
                );
                messagingTemplate.convertAndSend("/topic/worlds/" + slug, message);
            }
        });

        return world;
    }

    private void initializeBoard(World world, int boardSize) {
        List<Square> squares = new ArrayList<>();

        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                Square square = new Square();
                square.setWorld(world);
                square.setX(x);
                square.setY(y);
                square.setOwner(null);
                square.setDefenseBonus(0);
                squares.add(square);
            }
        }

        squareRepository.saveAll(squares);
    }

    private void resetBoard(World world, int newBoardSize) {
        // Delete old squares
        squareRepository.deleteByWorld(world);

        // FORCE deletion to complete before inserting new squares
        entityManager.flush();
        entityManager.clear();

        // Create new board
        initializeBoard(world, newBoardSize);
    }

    public List<World> getAllWorlds() {
        return worldRepository.findAll();
    }

    public Optional<World> getWorldBySlug(String slug) {
        return worldRepository.findBySlug(slug);
    }

    public List<Square> getWorldBoard(String worldSlug) {
        World world = worldRepository.findBySlug(worldSlug)
                .orElseThrow(() -> new RuntimeException("World not found"));
        return squareRepository.findByWorld(world);
    }
}