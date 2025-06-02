package vaultiq.session.core.scheduler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistenceRequirement;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.model.ModelType;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Schedules cleanup tasks for each session model type based on the configuration provided
 * in {@link VaultiqSessionProperties}. Supports both cron-based and fixed-delay scheduling,
 * as well as retention policies for cleanup.
 * <p>
 * The cleanup configuration is specified per model type using the nested {@code clean-up}
 * section in the configuration YAML, supporting fields such as {@code schedule}, {@code delay},
 * and {@code retention}.
 * </p>
 * <p>
 * Example YAML:
 * <pre>
 * vaultiq:
 *   session:
 *     persistence:
 *       models:
 *         - type: SESSION
 *           clean-up:
 *             schedule: "0 0 3 * * *"
 *             retention: "30d"
 *         - type: REVOKE
 *           clean-up:
 *             delay: "30m"
 *             retention: "90d"
 * </pre>
 */
@Component
@ConditionalOnVaultiqPersistenceRequirement(VaultiqPersistenceMethod.USE_JPA)
@ConditionalOnBean({TaskScheduler.class, CleanupTasks.class})
public class ModelCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(ModelCleanupScheduler.class);

    private final TaskScheduler taskScheduler;
    private final VaultiqSessionProperties sessionProperties;
    private final ScheduledExecutorService executorService;
    private final CleanupTasks cleanupTasks;

    /**
     * Constructs the scheduler with the required dependencies.
     *
     * @param taskScheduler     the Spring TaskScheduler for cron-based scheduling
     * @param sessionProperties the session configuration properties
     */
    public ModelCleanupScheduler(
            TaskScheduler taskScheduler,
            VaultiqSessionProperties sessionProperties,
            CleanupTasks cleanupTasks
    ) {
        this.taskScheduler = taskScheduler;
        this.sessionProperties = sessionProperties;
        this.cleanupTasks = cleanupTasks;
        var modelCount = sessionProperties.getPersistence().getModels().size();
        int threadPoolSize = Math.min(modelCount * 2, 10);
        this.executorService = Executors.newScheduledThreadPool(threadPoolSize);
        log.debug("Initialized cleanup scheduler with thread pool size: {}", threadPoolSize);
    }

    /**
     * Schedules cleanup tasks for each configured model type after bean initialization.
     * <p>
     * For each model, if a cron schedule is specified, a cron-based cleanup task is scheduled.
     * Else, if a delay is specified, a fixed-delay cleanup task is scheduled.
     * </p>
     */
    @PostConstruct
    public void scheduleModelCleanups() {
        log.debug("Initializing model cleanup schedules");

        List<VaultiqSessionProperties.ModelPersistenceConfig> models =
                sessionProperties.getPersistence().getModels();

        if (CollectionUtils.isEmpty(models)) {
            log.debug("No model configurations found for cleanup");
            return;
        }

        for (VaultiqSessionProperties.ModelPersistenceConfig model : models) {
            if (model.getType() == null) continue;

            VaultiqSessionProperties.CleanupConfig cleanup = model.getCleanUp();
            if (cleanup == null) continue;

            ModelType type = model.getType();
            var cron = cleanup.getSchedule();
            Duration delay = cleanup.getDelay();
            Duration retention = cleanup.getRetention() == null || cleanup.getRetention().isNegative()
                    ? Duration.ZERO
                    : cleanup.getRetention();

            // Schedule by cron if present
            if (cron != null && !cron.isBlank()) {
                log.info("Scheduling cron-based cleanup for model type: {} with cron: {}", type, cron);
                taskScheduler.schedule(
                        () -> cleanupTasks.cleanup(type, retention),
                        new CronTrigger(cron)
                );
            }
            // Else schedule by delay if present
            else if (delay != null && !delay.isZero() && !delay.isNegative()) {
                log.info("Scheduling fixed-delay cleanup for model type: {} with delay: {}", type, delay);
                executorService.scheduleWithFixedDelay(
                        () -> cleanupTasks.cleanup(type, retention),
                        0,
                        delay.toMillis(),
                        TimeUnit.MILLISECONDS
                );
            }
        }
    }

    /**
     * Shuts down the executor service when the bean is destroyed.
     * <p>
     */
    @PreDestroy
    public void shutdownExecutor() {
        log.debug("Shutting down cleanup scheduler executor service");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in time, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("Executor shutdown interrupted", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
