package vaultiq.session.core.scheduler;

import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.model.ModelType;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ModelCleanupScheduler {

    private final TaskScheduler taskScheduler;
    private final VaultiqSessionProperties sessionProperties;

    public ModelCleanupScheduler(TaskScheduler taskScheduler, VaultiqSessionProperties sessionProperties) {
        this.taskScheduler = taskScheduler;
        this.sessionProperties = sessionProperties;
    }

    @PostConstruct
    public void scheduleModelCleanups() {
        List<VaultiqSessionProperties.ModelPersistenceConfig> models =
                sessionProperties.getPersistence().getModels();

        for (VaultiqSessionProperties.ModelPersistenceConfig model : models) {
            String dbCleanup = model.getDbCleanup();
            if (dbCleanup == null || dbCleanup.isBlank()) continue;

            ModelType type = model.getType();

            CleanupStrategyParser.parseCron(dbCleanup).ifPresent(cronTrigger ->
                    taskScheduler.schedule(() -> clean(type), cronTrigger)
            );

            CleanupStrategyParser.parseDuration(dbCleanup).ifPresent(duration -> {
                try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                    executor.scheduleWithFixedDelay(() -> clean(type), 0, duration.toMillis(), TimeUnit.MILLISECONDS);
                }
            });
        }
    }

    private void clean(ModelType modelType) {
        // TODO: should implement logic to clean up the specified model type
    }
}

