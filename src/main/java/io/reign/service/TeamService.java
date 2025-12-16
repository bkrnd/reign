package io.reign.service;

import io.reign.dto.TeamResponse;
import io.reign.model.Square;
import io.reign.model.SquareUpdateMessage;
import io.reign.model.Team;
import io.reign.model.TeamMember;
import io.reign.model.User;
import io.reign.model.World;
import io.reign.repository.SquareRepository;
import io.reign.repository.TeamMemberRepository;
import io.reign.repository.TeamRepository;
import io.reign.repository.UserRepository;
import io.reign.repository.WorldRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SquareRepository squareRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Join an existing team in a world
     */
    @Transactional
    public TeamResponse joinTeam(String worldSlug, String teamId, String userId) {
        // Get world
        World world = worldRepository.findBySlugWithTeamsAndMembers(worldSlug)
                .orElseThrow(() -> new IllegalArgumentException("World not found"));

        // Validate world is public
        if (!world.isPublic()) {
            throw new IllegalStateException("Cannot join a private world");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if user is already in a team in this world
        Optional<TeamMember> existingMembership = teamMemberRepository.findByUserIdAndWorldId(userId, world.getId());
        if (existingMembership.isPresent()) {
            throw new IllegalStateException("User is already in a team in this world");
        }

        // Get team
        Team team = teamRepository.findByIdWithMembers(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        // Verify team belongs to this world
        if (!team.getWorld().getId().equals(world.getId())) {
            throw new IllegalArgumentException("Team does not belong to this world");
        }

        // Check if team is full
        long currentMemberCount = teamMemberRepository.countByTeamId(teamId);
        if (currentMemberCount >= world.getMaxTeamSize()) {
            throw new IllegalStateException("Team is full");
        }

        // Create team membership
        TeamMember teamMember = new TeamMember();
        teamMember.setUser(user);
        teamMember.setTeam(team);
        teamMember.setJoinedAt(LocalDateTime.now());
        teamMember.setCreatedAt(LocalDateTime.now());

        teamMemberRepository.save(teamMember);

        // Flush to ensure changes are persisted before re-fetching
        entityManager.flush();

        // Return the updated team as DTO (within transaction context)
        Team updatedTeam = teamRepository.findByIdWithMembers(teamId)
                .orElseThrow(() -> new RuntimeException("Failed to retrieve updated team"));

        // Broadcast team update AFTER transaction commits
        String finalWorldSlug = worldSlug;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Clear entity manager to ensure fresh data
                entityManager.clear();

                // Fetch entire board state and teams with FRESH data
                World updatedWorld = worldRepository.findBySlugWithTeamsAndMembers(finalWorldSlug)
                        .orElseThrow(() -> new RuntimeException("World not found after commit"));
                List<Square> board = squareRepository.findByWorld(updatedWorld);

                // Initialize lazy collections before sending
                updatedWorld.getTeams().forEach(team -> {
                    team.getMembers().size();
                    team.getMembers().forEach(member -> member.getUser().getUsername());
                });

                SquareUpdateMessage message = new SquareUpdateMessage(
                    "TEAM_JOINED",
                    board,
                    updatedWorld.getTeams(),
                    userId,
                    System.currentTimeMillis()
                );
                messagingTemplate.convertAndSend("/topic/worlds/" + finalWorldSlug, message);
            }
        });

        return TeamResponse.fromTeam(updatedTeam);
    }

    /**
     * Create a new team in a world
     */
    @Transactional
    public TeamResponse createTeam(String worldSlug, String teamName, String color, String userId) {
        // Get world
        World world = worldRepository.findBySlugWithTeamsAndMembers(worldSlug)
                .orElseThrow(() -> new IllegalArgumentException("World not found"));

        // Validate world is public
        if (!world.isPublic()) {
            throw new IllegalStateException("Cannot join a private world");
        }

        // Check if player team creation is allowed
        if (!world.isAllowPlayerTeamCreation()) {
            throw new IllegalStateException("Player team creation is not allowed in this world");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if user is already in a team in this world
        Optional<TeamMember> existingMembership = teamMemberRepository.findByUserIdAndWorldId(userId, world.getId());
        if (existingMembership.isPresent()) {
            throw new IllegalStateException("User is already in a team in this world");
        }

        // Check if max teams reached
        long currentTeamCount = teamRepository.countByWorldId(world.getId());
        if (currentTeamCount >= world.getMaxTeams()) {
            throw new IllegalStateException("Maximum number of teams reached for this world");
        }

        // Check if team name already exists in this world
        if (teamRepository.existsByWorldIdAndName(world.getId(), teamName)) {
            throw new IllegalStateException("Team name already exists in this world");
        }

        // Validate color (should match enum values in Team model)
        if (!isValidColor(color)) {
            throw new IllegalArgumentException("Invalid team color. Must be one of: red, blue, green, yellow, purple, teal");
        }

        // Check if color is already taken in this world
        boolean colorTaken = world.getTeams().stream()
                .anyMatch(t -> t.getColor().equalsIgnoreCase(color));
        if (colorTaken) {
            throw new IllegalStateException("Color is already taken by another team");
        }

        // Create team
        Team team = new Team();
        team.setName(teamName);
        team.setColor(color.toLowerCase());
        team.setWorld(world);
        team.setCreator(user);
        team.setCreatedAt(LocalDateTime.now());

        Team savedTeam = teamRepository.save(team);

        // Create team membership for creator
        TeamMember teamMember = new TeamMember();
        teamMember.setUser(user);
        teamMember.setTeam(savedTeam);
        teamMember.setJoinedAt(LocalDateTime.now());
        teamMember.setCreatedAt(LocalDateTime.now());

        teamMemberRepository.save(teamMember);

        // Flush to ensure changes are persisted before re-fetching
        entityManager.flush();

        // Return the team with members as DTO (within transaction context)
        Team createdTeam = teamRepository.findByIdWithMembers(savedTeam.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created team"));

        // Broadcast team creation AFTER transaction commits
        String finalWorldSlug = worldSlug;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Clear entity manager to ensure fresh data
                entityManager.clear();

                // Fetch entire board state and teams with FRESH data
                World updatedWorld = worldRepository.findBySlugWithTeamsAndMembers(finalWorldSlug)
                        .orElseThrow(() -> new RuntimeException("World not found after commit"));
                List<Square> board = squareRepository.findByWorld(updatedWorld);

                System.out.println("[TeamService] Broadcasting TEAM_CREATED for world: " + finalWorldSlug);
                System.out.println("[TeamService] Teams count: " + updatedWorld.getTeams().size());
                System.out.println("[TeamService] Team IDs: " + updatedWorld.getTeams().stream().map(Team::getId).toList());
                System.out.println("[TeamService] Board squares count: " + board.size());

                // Initialize lazy collections before sending
                updatedWorld.getTeams().forEach(team -> {
                    team.getMembers().size(); // Force load members
                    team.getMembers().forEach(member -> {
                        member.getUser().getUsername(); // Force load user
                    });
                });

                SquareUpdateMessage message = new SquareUpdateMessage(
                    "TEAM_CREATED",
                    board,
                    updatedWorld.getTeams(),
                    userId,
                    System.currentTimeMillis()
                );
                messagingTemplate.convertAndSend("/topic/worlds/" + finalWorldSlug, message);
                System.out.println("[TeamService] Broadcast sent to /topic/worlds/" + finalWorldSlug);
            }
        });

        return TeamResponse.fromTeam(createdTeam);
    }

    /**
     * Get available teams for a world (teams that are not full)
     */
    public List<Team> getAvailableTeams(String worldSlug) {
        World world = worldRepository.findBySlugWithTeamsAndMembers(worldSlug)
                .orElseThrow(() -> new IllegalArgumentException("World not found"));

        return world.getTeams().stream()
                .filter(team -> {
                    long memberCount = teamMemberRepository.countByTeamId(team.getId());
                    return memberCount < world.getMaxTeamSize();
                })
                .toList();
    }

    /**
     * Get all teams for a world
     */
    public List<Team> getTeamsByWorld(String worldSlug) {
        World world = worldRepository.findBySlug(worldSlug)
                .orElseThrow(() -> new IllegalArgumentException("World not found"));

        return teamRepository.findByWorldId(world.getId());
    }

    /**
     * Check if user is in a team in a world
     */
    public boolean isUserInTeam(String worldSlug, String userId) {
        World world = worldRepository.findBySlug(worldSlug)
                .orElseThrow(() -> new IllegalArgumentException("World not found"));

        return teamMemberRepository.findByUserIdAndWorldId(userId, world.getId()).isPresent();
    }

    /**
     * Get the team that a user is in for a specific world (as entity)
     */
    public Optional<Team> getUserTeamInWorld(String worldSlug, String userId) {
        World world = worldRepository.findBySlug(worldSlug)
                .orElseThrow(() -> new IllegalArgumentException("World not found"));

        Optional<TeamMember> membership = teamMemberRepository.findByUserIdAndWorldId(userId, world.getId());
        return membership.map(TeamMember::getTeam);
    }

    /**
     * Get the team that a user is in for a specific world (as DTO)
     */
    @Transactional(readOnly = true)
    public Optional<TeamResponse> getUserTeamResponseInWorld(String worldSlug, String userId) {
        World world = worldRepository.findBySlug(worldSlug)
                .orElseThrow(() -> new IllegalArgumentException("World not found"));

        Optional<TeamMember> membership = teamMemberRepository.findByUserIdAndWorldId(userId, world.getId());
        if (membership.isEmpty()) {
            return Optional.empty();
        }

        // Fetch team with all relationships eagerly
        Team team = teamRepository.findByIdWithMembers(membership.get().getTeam().getId())
                .orElseThrow(() -> new RuntimeException("Team not found"));

        return Optional.of(TeamResponse.fromTeam(team));
    }

    /**
     * Leave a team in a world
     */
    @Transactional
    public void leaveTeam(String worldSlug, String userId) {
        World world = worldRepository.findBySlug(worldSlug)
                .orElseThrow(() -> new IllegalArgumentException("World not found"));

        // Check if user is in a team in this world
        TeamMember membership = teamMemberRepository.findByUserIdAndWorldId(userId, world.getId())
                .orElseThrow(() -> new IllegalStateException("User is not in a team in this world"));

        // Reset all squares owned by this user in this world
        squareRepository.resetSquaresByUserAndWorld(userId, world);

        // Remove the membership
        teamMemberRepository.delete(membership);

        // If the team is now empty, delete it
        long remainingMembers = teamMemberRepository.countByTeamId(membership.getTeam().getId());
        if (remainingMembers == 0) {
            teamRepository.delete(membership.getTeam());
        }

        // Broadcast square changes and team updates AFTER transaction commits
        String finalWorldSlug = worldSlug;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Clear entity manager to ensure fresh data
                entityManager.clear();

                // Fetch entire board state and teams with FRESH data
                World updatedWorld = worldRepository.findBySlugWithTeamsAndMembers(finalWorldSlug)
                        .orElseThrow(() -> new RuntimeException("World not found after commit"));
                List<Square> board = squareRepository.findByWorld(updatedWorld);

                // Initialize lazy collections before sending
                updatedWorld.getTeams().forEach(team -> {
                    team.getMembers().size();
                    team.getMembers().forEach(member -> member.getUser().getUsername());
                });

                SquareUpdateMessage message = new SquareUpdateMessage(
                    "PLAYER_LEFT",
                    board,
                    updatedWorld.getTeams(),
                    userId,
                    System.currentTimeMillis()
                );
                messagingTemplate.convertAndSend("/topic/worlds/" + finalWorldSlug, message);
            }
        });
    }

    /**
     * Cleanup user data when user is deleted
     * - Resets all squares owned by the user across all worlds
     * - Deletes empty teams after user leaves
     * - Broadcasts changes to all affected worlds
     */
    @Transactional
    public void cleanupUserData(String userId) {
        // Get all team memberships for this user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<TeamMember> memberships = teamMemberRepository.findByUserId(userId);

        // Reset all squares owned by this user across all worlds
        squareRepository.resetSquaresByUser(userId);

        // Track worlds that need broadcasting
        List<String> affectedWorldSlugs = new java.util.ArrayList<>();

        // Process each team membership
        for (TeamMember membership : memberships) {
            Team team = membership.getTeam();
            World world = team.getWorld();
            affectedWorldSlugs.add(world.getSlug());

            // Remove the membership
            teamMemberRepository.delete(membership);

            // Check if team is now empty and delete it
            long remainingMembers = teamMemberRepository.countByTeamId(team.getId());
            if (remainingMembers == 0) {
                teamRepository.delete(team);
            }
        }

        // Broadcast changes to all affected worlds AFTER transaction commits
        if (!affectedWorldSlugs.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Clear entity manager to ensure fresh data
                    entityManager.clear();

                    for (String worldSlug : affectedWorldSlugs) {
                        // Fetch world and board state with teams with FRESH data
                        worldRepository.findBySlugWithTeamsAndMembers(worldSlug).ifPresent(world -> {
                            List<Square> board = squareRepository.findByWorld(world);

                            // Initialize lazy collections before sending
                            world.getTeams().forEach(team -> {
                                team.getMembers().size();
                                team.getMembers().forEach(member -> member.getUser().getUsername());
                            });

                            SquareUpdateMessage message = new SquareUpdateMessage(
                                "USER_DELETED",
                                board,
                                world.getTeams(),
                                userId,
                                System.currentTimeMillis()
                            );
                            messagingTemplate.convertAndSend("/topic/worlds/" + worldSlug, message);
                        });
                    }
                }
            });
        }
    }

    /**
     * Validate if color is valid
     */
    private boolean isValidColor(String color) {
        return color != null && color.matches("^(red|blue|green|yellow|purple|teal)$");
    }
}
