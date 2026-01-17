package io.reign.service;

import io.reign.model.Square;
import io.reign.model.SquareUpdateMessage;
import io.reign.model.Team;
import io.reign.model.TeamMember;
import io.reign.model.World;
import io.reign.repository.SquareRepository;
import io.reign.repository.TeamMemberRepository;
import io.reign.repository.WorldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Service
public class CycleSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(CycleSchedulerService.class);

    private final Map<String, ScheduledFuture<?>> worldTasks = new HashMap<>();

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private SquareRepository squareRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Start the cycle scheduler for a world.
     * Calculates the delay until the next cycle and schedules the task.
     */
    public void startWorldCycle(World world) {
        stopWorldCycle(world.getId());

        Duration delay = calculateTimeUntilNextCycle(world);
        logger.info("Starting cycle for world '{}' (id: {}). Next cycle in {} seconds",
                world.getSlug(), world.getId(), delay.getSeconds());

        ScheduledFuture<?> task = taskScheduler.schedule(
                () -> processCycleAndReschedule(world.getId()),
                Instant.now().plus(delay)
        );

        worldTasks.put(world.getId(), task);
    }

    /**
     * Stop the cycle scheduler for a world.
     */
    public void stopWorldCycle(String worldId) {
        ScheduledFuture<?> existingTask = worldTasks.remove(worldId);
        if (existingTask != null) {
            existingTask.cancel(false);
            logger.info("Stopped cycle for world id: {}", worldId);
        }
    }

    /**
     * Reschedule the cycle for a world (e.g., after world reset or config change).
     */
    public void rescheduleWorldCycle(World world) {
        startWorldCycle(world);
    }

    /**
     * Calculate time until the next cycle for a world.
     */
    public Duration calculateTimeUntilNextCycle(World world) {
        Instant cycleStartedAt = world.getCycleStartedAt();
        Duration cycleDuration = Duration.ofSeconds(world.getCycleDurationInSeconds());

        Duration elapsed = Duration.between(cycleStartedAt, Instant.now());
        long cyclesPassed = elapsed.toMillis() / cycleDuration.toMillis();

        Instant nextCycleAt = cycleStartedAt.plus(cycleDuration.multipliedBy(cyclesPassed + 1));
        Duration delay = Duration.between(Instant.now(), nextCycleAt);

        // Ensure minimum delay of 1 second to avoid immediate execution issues
        if (delay.isNegative() || delay.isZero()) {
            delay = Duration.ofSeconds(1);
        }

        return delay;
    }

    /**
     * Get the next cycle time for a world.
     */
    public Instant getNextCycleTime(World world) {
        Instant cycleStartedAt = world.getCycleStartedAt();
        Duration cycleDuration = Duration.ofSeconds(world.getCycleDurationInSeconds());

        Duration elapsed = Duration.between(cycleStartedAt, Instant.now());
        long cyclesPassed = elapsed.toMillis() / cycleDuration.toMillis();

        return cycleStartedAt.plus(cycleDuration.multipliedBy(cyclesPassed + 1));
    }

    /**
     * Process a cycle tick: regenerate action points and broadcast update.
     */
    @Transactional
    public void processCycleAndReschedule(String worldId) {
        try {
            World world = worldRepository.findById(worldId).orElse(null);
            if (world == null) {
                logger.warn("World {} not found, removing from scheduler", worldId);
                worldTasks.remove(worldId);
                return;
            }

            logger.info("Processing cycle for world '{}' (id: {})", world.getSlug(), worldId);

            // Regenerate action points for all team members in this world
            regenerateActionPoints(world);

            // Update cycleStartedAt to now
            world.setCycleStartedAt(Instant.now());
            worldRepository.save(world);

            // Broadcast cycle tick
            broadcastCycleTick(world);

            // Reschedule next cycle
            Duration nextDelay = Duration.ofSeconds(world.getCycleDurationInSeconds());
            logger.info("Scheduling next cycle for world '{}' in {} seconds",
                    world.getSlug(), world.getCycleDurationInSeconds());

            ScheduledFuture<?> task = taskScheduler.schedule(
                    () -> processCycleAndReschedule(worldId),
                    Instant.now().plus(nextDelay)
            );
            worldTasks.put(worldId, task);

        } catch (Exception e) {
            logger.error("Error processing cycle for world {}: {}", worldId, e.getMessage(), e);
            // Try to reschedule anyway to avoid losing the cycle
            World world = worldRepository.findById(worldId).orElse(null);
            if (world != null) {
                Duration retryDelay = Duration.ofSeconds(world.getCycleDurationInSeconds());
                ScheduledFuture<?> task = taskScheduler.schedule(
                        () -> processCycleAndReschedule(worldId),
                        Instant.now().plus(retryDelay)
                );
                worldTasks.put(worldId, task);
            }
        }
    }

    /**
     * Regenerate action points for all team members in a world.
     */
    private void regenerateActionPoints(World world) {
        World worldWithTeams = worldRepository.findBySlugWithTeamsAndMembers(world.getSlug())
                .orElse(world);

        int pointsToAdd = world.getActionPointsPerCycle();
        int maxPoints = world.getMaxActionPoints();

        for (Team team : worldWithTeams.getTeams()) {
            for (TeamMember member : team.getMembers()) {
                int newPoints = Math.min(member.getCurrentActionPoints() + pointsToAdd, maxPoints);
                member.setCurrentActionPoints(newPoints);
                teamMemberRepository.save(member);
            }
        }

        logger.info("Regenerated {} action points for all members in world '{}' (max: {})",
                pointsToAdd, world.getSlug(), maxPoints);
    }

    /**
     * Broadcast cycle tick to all connected clients.
     */
    private void broadcastCycleTick(World world) {
        World worldWithTeams = worldRepository.findBySlugWithTeamsAndMembers(world.getSlug())
                .orElse(world);
        List<Square> board = squareRepository.findByWorld(world);

        // Initialize lazy collections
        worldWithTeams.getTeams().forEach(team -> {
            team.getMembers().size();
            team.getMembers().forEach(member -> member.getUser().getUsername());
        });

        Instant nextCycleAt = getNextCycleTime(world);

        SquareUpdateMessage message = new SquareUpdateMessage(
                "CYCLE_TICK",
                board,
                worldWithTeams.getTeams(),
                null,
                System.currentTimeMillis()
        );
        message.setNextCycleAt(nextCycleAt.toEpochMilli());

        messagingTemplate.convertAndSend("/topic/worlds/" + world.getSlug(), message);
        logger.info("Broadcast CYCLE_TICK for world '{}'", world.getSlug());
    }

    /**
     * Check if a world has an active cycle scheduler.
     */
    public boolean isWorldScheduled(String worldId) {
        ScheduledFuture<?> task = worldTasks.get(worldId);
        return task != null && !task.isCancelled() && !task.isDone();
    }

    /**
     * Get info about all scheduled world cycles (for debugging).
     */
    public List<Map<String, Object>> getScheduledWorldsInfo() {
        List<Map<String, Object>> result = new java.util.ArrayList<>();

        for (Map.Entry<String, ScheduledFuture<?>> entry : worldTasks.entrySet()) {
            String worldId = entry.getKey();
            ScheduledFuture<?> task = entry.getValue();

            World world = worldRepository.findById(worldId).orElse(null);

            Map<String, Object> info = new java.util.HashMap<>();
            info.put("worldId", worldId);
            info.put("worldSlug", world != null ? world.getSlug() : "unknown");
            info.put("worldName", world != null ? world.getName() : "unknown");
            info.put("isCancelled", task.isCancelled());
            info.put("isDone", task.isDone());
            info.put("isActive", !task.isCancelled() && !task.isDone());

            if (world != null) {
                info.put("cycleDurationSeconds", world.getCycleDurationInSeconds());
                info.put("nextCycleAt", getNextCycleTime(world).toString());
                info.put("timeUntilNextCycle", calculateTimeUntilNextCycle(world).toString());
            }

            result.add(info);
        }

        return result;
    }
}
