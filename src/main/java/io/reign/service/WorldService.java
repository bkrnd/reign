package io.reign.service;

import io.reign.enums.BoardType;
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
    public World createWorld(String slug, String name, String ownerId, BoardType boardType ,Integer boardSize, Integer maxPlayers,
                            Integer maxTeams, Integer minTeams, Integer maxTeamSize, Integer minTeamSize,
                            Boolean allowPlayerTeamCreation, Boolean isPublic) {
        World world = new World();
        world.setSlug(slug);
        world.setName(name);
        if (ownerId != null) {
            User owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            world.setOwner(owner);
        }
        world.setBoardType(boardType != null ? boardType : BoardType.HEXAGON);
        world.setBoardSize(boardSize != null ? boardSize : 20);
        world.setMaxPlayers(maxPlayers != null ? maxPlayers : 6);
        world.setMaxTeams(maxTeams != null ? maxTeams : 6);
        world.setMinTeams(minTeams != null ? minTeams : 2);
        world.setMaxTeamSize(maxTeamSize != null ? maxTeamSize : 3);
        world.setMinTeamSize(minTeamSize != null ? minTeamSize : 1);
        world.setAllowPlayerTeamCreation(allowPlayerTeamCreation != null ? allowPlayerTeamCreation : true);
        world.setPublic(isPublic != null ? isPublic : true);

        World saved = worldRepository.save(world);

        initializeBoard(saved, saved.getBoardSize());

        return saved;
    }

    @Transactional
    public World updateWorld(String slug, String name, String ownerId, BoardType boardType ,Integer boardSize, Integer maxPlayers,
                            Integer maxTeams, Integer minTeams, Integer maxTeamSize, Integer minTeamSize,
                            Boolean allowPlayerTeamCreation, Boolean isPublic) {
        World world = worldRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("World not found"));

        int oldBoardSize = world.getBoardSize();

        if (name != null) world.setName(name);
        if (ownerId != null) {
            User owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            world.setOwner(owner);
        }
        if (boardType != null) world.setBoardType(boardType);
        if (boardSize != null) world.setBoardSize(boardSize);
        if (maxPlayers != null) world.setMaxPlayers(maxPlayers);
        if (maxTeams != null) world.setMaxTeams(maxTeams);
        if (minTeams != null) world.setMinTeams(minTeams);
        if (maxTeamSize != null) world.setMaxTeamSize(maxTeamSize);
        if (minTeamSize != null) world.setMinTeamSize(minTeamSize);
        if (allowPlayerTeamCreation != null) world.setAllowPlayerTeamCreation(allowPlayerTeamCreation);
        if (isPublic != null) world.setPublic(isPublic);

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
                // Fetch entire board state and teams
                World updatedWorld = worldRepository.findBySlugWithTeamsAndMembers(slug).orElse(world);
                List<Square> board = squareRepository.findByWorld(world);

                SquareUpdateMessage message = new SquareUpdateMessage(
                    "WORLD_RESET",
                    board,
                    updatedWorld.getTeams(),
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
        return worldRepository.findAllWithTeamsAndMembers();
    }

    public List<World> getPublicWorlds() {
        return worldRepository.findByIsPublicTrueWithTeamsAndMembers();
    }

    public List<World> getWorldsForUser(String userId) {
        if (userId == null) {
            return getPublicWorlds();
        }
        return worldRepository.findPublicOrOwnedByUserWithTeamsAndMembers(userId);
    }

    public List<World> getWorldsWithFilters(String userId, Boolean isPublic, BoardType boardType, String search, Boolean hideFull) {
        // First, get all worlds the user has access to
        List<World> worlds;
        if (userId == null) {
            worlds = worldRepository.findByIsPublicTrueWithTeamsAndMembers();
        } else {
            worlds = worldRepository.findPublicOrOwnedByUserWithTeamsAndMembers(userId);
        }

        // Apply filters in Java
        return worlds.stream()
            .filter(world -> {
                // Filter by isPublic
                if (isPublic != null && world.isPublic() != isPublic) {
                    return false;
                }

                // Filter by boardType
                if (boardType != null && world.getBoardType() != boardType) {
                    return false;
                }

                // Filter by search (case-insensitive contains)
                if (search != null && !search.trim().isEmpty()) {
                    if (!world.getName().toLowerCase().contains(search.trim().toLowerCase())) {
                        return false;
                    }
                }

                // Filter by hideFull
                if (hideFull != null && hideFull) {
                    int currentPlayers = world.getTeams().stream()
                        .mapToInt(team -> team.getMembers().size())
                        .sum();
                    if (currentPlayers >= world.getMaxPlayers()) {
                        return false;
                    }
                }

                return true;
            })
            .toList();
    }

    public Optional<World> getWorldBySlug(String slug) {
        return worldRepository.findBySlugWithTeamsAndMembers(slug);
    }

    public List<Square> getWorldBoard(String worldSlug) {
        World world = worldRepository.findBySlugWithTeamsAndMembers(worldSlug)
                .orElseThrow(() -> new RuntimeException("World not found"));
        return squareRepository.findByWorld(world);
    }
}