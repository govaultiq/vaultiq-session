package vaultiq.session.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

public class VaultiqPersistenceCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(ConditionalOnVaultiqPersistence.class.getName());
        if (attributes == null) return false;

        Object modeObj = attributes.getFirst("mode");
        if (modeObj == null) return false;

        VaultiqPersistenceMode mode = (VaultiqPersistenceMode) modeObj;

        boolean jpa = Boolean.parseBoolean(context.getEnvironment().getProperty("vaultiq.session.persistence.jpa.enabled", "false"));
        boolean cache = Boolean.parseBoolean(context.getEnvironment().getProperty("vaultiq.session.persistence.cache.enabled", "false"));

        return switch (mode) {
            case JPA_ONLY -> jpa && !cache;
            case CACHE_ONLY -> cache && !jpa;
            case JPA_AND_CACHE -> jpa && cache;
        };
    }
}
