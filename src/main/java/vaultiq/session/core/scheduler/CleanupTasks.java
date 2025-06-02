package vaultiq.session.core.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistenceRequirement;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.jpa.revoke.service.internal.RevokedSessionEntityService;
import vaultiq.session.jpa.session.service.internal.ClientSessionEntityService;
import vaultiq.session.model.ModelType;

import java.time.Duration;

/**
 * Executes cleanup tasks for different model types.
 * Provides logging for monitoring cleanup operations.
 */
@Component
@ConditionalOnVaultiqPersistenceRequirement(VaultiqPersistenceMethod.USE_JPA)
@ConditionalOnBean({RevokedSessionEntityService.class, ClientSessionEntityService.class})
public final class CleanupTasks {
    private static final Logger log = LoggerFactory.getLogger(CleanupTasks.class);
    
    private final RevokedSessionEntityService revokedSessionEntityService;
    private final ClientSessionEntityService clientSessionEntityService;

    public CleanupTasks(
            RevokedSessionEntityService revokedSessionEntityService,
            ClientSessionEntityService clientSessionEntityService
    ) {
        this.revokedSessionEntityService = revokedSessionEntityService;
        this.clientSessionEntityService = clientSessionEntityService;
    }

    /**
     * Performs cleanup for a specific model type with the given retention period.
     *
     * @param modelType the model type to clean up
     * @param retentionPeriod the retention period
     * @return the number of records deleted
     */
    public int cleanup(ModelType modelType, Duration retentionPeriod) {
        if (retentionPeriod == null || retentionPeriod.isNegative() || retentionPeriod.isZero()) {
            log.warn("Skipping cleanup for model type {} due to invalid retention period: {}", 
                    modelType, retentionPeriod);
            return 0;
        }
        
        log.info("Starting cleanup for model type: {} with retention period: {}", modelType, retentionPeriod);
        
        int deletedCount;
        
        try {
            deletedCount = switch (modelType) {
                case SESSION -> clientSessionEntityService.deleteAllRevokedSessions(retentionPeriod);
                case REVOKE -> revokedSessionEntityService.deleteAllRevokedSessions(retentionPeriod);
            };
            
            log.info("Completed cleanup for model type: {}. Deleted {} records", modelType, deletedCount);
            
            return deletedCount;
        } catch (Exception e) {
            log.error("Error during cleanup for model type: {}", modelType, e);
            throw e;
        }
    }
}