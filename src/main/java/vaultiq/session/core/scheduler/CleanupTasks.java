package vaultiq.session.core.scheduler;

import org.springframework.stereotype.Component;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistenceRequirement;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.jpa.revoke.service.internal.RevokedSessionEntityService;
import vaultiq.session.jpa.session.service.internal.ClientSessionEntityService;
import vaultiq.session.model.ModelType;

import java.time.Duration;

@Component
@ConditionalOnVaultiqPersistenceRequirement(VaultiqPersistenceMethod.USE_JPA)
public final class CleanupTasks {
    private final RevokedSessionEntityService revokedSessionEntityService;
    private final ClientSessionEntityService clientSessionEntityService;

    public CleanupTasks(
            RevokedSessionEntityService revokedSessionEntityService,
            ClientSessionEntityService clientSessionEntityService
    ) {
        this.revokedSessionEntityService = revokedSessionEntityService;
        this.clientSessionEntityService = clientSessionEntityService;
    }

    public void cleanup(ModelType modelType, Duration retentionPeriod) {
        switch (modelType) {
            case SESSION -> clientSessionEntityService.deleteAllRevokedSessions(retentionPeriod);
            case REVOKE -> revokedSessionEntityService.deleteAllRevokedSessions(retentionPeriod);
        }
    }
}
