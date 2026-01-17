package io.reign.config;

import io.reign.model.World;
import io.reign.repository.WorldRepository;
import io.reign.service.CycleSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class CycleSchedulerInitializer {

    private static final Logger logger = LoggerFactory.getLogger(CycleSchedulerInitializer.class);

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private CycleSchedulerService cycleSchedulerService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeWorldCycles() {
        logger.info("Initializing cycle schedulers for all worlds...");

        List<World> worlds = worldRepository.findAll();
        int scheduledCount = 0;

        for (World world : worlds) {
            try {
                // Initialize cycleStartedAt for existing worlds that don't have it
                if (world.getCycleStartedAt() == null) {
                    logger.info("Initializing cycleStartedAt for world '{}' (id: {})",
                            world.getSlug(), world.getId());
                    world.setCycleStartedAt(Instant.now());
                    worldRepository.save(world);
                }

                cycleSchedulerService.startWorldCycle(world);
                scheduledCount++;
            } catch (Exception e) {
                logger.error("Failed to start cycle for world '{}' (id: {}): {}",
                        world.getSlug(), world.getId(), e.getMessage(), e);
            }
        }

        logger.info("Initialized cycle schedulers for {} worlds", scheduledCount);

        // Log all scheduled cycles
        logger.info("=== SCHEDULED CYCLES ===");
        for (var info : cycleSchedulerService.getScheduledWorldsInfo()) {
            logger.info("  World: {} ({}) - Next cycle: {} - Duration: {}min - Active: {}",
                info.get("worldSlug"),
                info.get("worldId"),
                info.get("nextCycleAt"),
                info.get("cycleDurationSeconds"),
                info.get("isActive"));
        }
        logger.info("========================");
    }
}
