package io.reign.service;

import io.reign.enums.BoardType;
import io.reign.model.Square;
import io.reign.model.SquareUpdateMessage;
import io.reign.model.Team;
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
import java.util.Optional;

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

    @Autowired
    private TeamService teamService;

    @PreAuthorize("hasPermission(#worldSlug, 'WORLD_MEMBER')")
    @Transactional
    public Square captureSquare(String worldSlug, int x, int y, String playerId) {
        // Fetch world
        World world = worldRepository.findBySlug(worldSlug)
                .orElseThrow(() -> new IllegalArgumentException("World not found"));

        // Verify player is in a team
        Optional<Team> playerTeam = teamService.getUserTeamInWorld(worldSlug, playerId);
        if (playerTeam.isEmpty()) {
            throw new IllegalStateException("Player must be in a team to perform actions");
        }

        // Fetch player
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        // Find the square
        Square square = squareRepository.findByWorldAndXAndY(world, x, y)
                .orElseThrow(() -> new IllegalArgumentException("Square not found"));

        // Check if team have any squares
        long teamSquareCount = squareRepository.countByWorldAndOwnerTeam(world, playerTeam.get());

        // Team can capture the square only neighboring squares they own
        boolean hasNeighborOwned = false;
        int[][] directions;
        if (world.getBoardType().equals(BoardType.HEXAGON)){
            directions = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {(y % 2 == 0) ? -1 : 1, 1}, {(y % 2 == 0) ? -1 : 1, -1}};
        } else {
            directions = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        }

        for (int[] dir : directions) {
            int neighborX = x + dir[0];
            int neighborY = y + dir[1];
            Optional<Square> neighborOpt = squareRepository.findByWorldAndXAndY(world, neighborX, neighborY);
            if (neighborOpt.isPresent()) {
                Square neighbor = neighborOpt.get();
                if (neighbor.getOwner() != null) {
                    Optional<Team> neighborOwnerTeam = teamService.getUserTeamInWorld(worldSlug, neighbor.getOwner().getId());
                    if (neighborOwnerTeam.isPresent() && neighborOwnerTeam.get().getId().equals(playerTeam.get().getId())) {
                        hasNeighborOwned = true;
                        break;
                    }
                }
            }
        }

        if (!hasNeighborOwned && teamSquareCount > 0) {
            throw new IllegalStateException("Team must own a neighboring square to capture this square");
        }

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

        // Preventing capturing if team has no squares
        if (teamSquareCount == 0) {
            throw new IllegalStateException("Team must own at least one square to capture an enemy square");
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

        // Verify player is in a team
        Optional<Team> playerTeam = teamService.getUserTeamInWorld(worldSlug, playerId);
        if (playerTeam.isEmpty()) {
            throw new IllegalStateException("Player must be in a team to perform actions");
        }

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
                // Fetch world and entire board state with teams
                World world = worldRepository.findBySlugWithTeamsAndMembers(worldSlug)
                        .orElseThrow(() -> new IllegalArgumentException("World not found"));
                List<Square> board = squareRepository.findByWorld(world);

                SquareUpdateMessage message = new SquareUpdateMessage(
                    messageType,
                    board,
                    world.getTeams(),
                    playerId,
                    System.currentTimeMillis()
                );
                messagingTemplate.convertAndSend("/topic/worlds/" + worldSlug, message);
            }
        });
    }
}