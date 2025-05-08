package vaultiq.session.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.core.VaultiqSessionContext;

import java.util.Map;
import java.util.Objects;

public class VaultiqBeanCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnVaultiqPersistence.class.getName());
        if (attributes == null) return false;

        VaultiqPersistenceMode mode = (VaultiqPersistenceMode) attributes.get("mode");
        ModelType modelType = (ModelType) attributes.get("type");

        VaultiqSessionContext sessionContext = Objects.requireNonNull(context.getBeanFactory()).getBean(VaultiqSessionContext.class);

        return sessionContext.getModelConfigs().stream()
                .filter(cfg -> cfg.modelType() == modelType)
                .anyMatch(cfg -> switch (mode) {
                    case CACHE_ONLY -> cfg.useCache() && !cfg.useJpa();
                    case JPA_ONLY -> !cfg.useCache() && cfg.useJpa();
                    case JPA_AND_CACHE -> cfg.useCache() && cfg.useJpa();
                });
    }
}