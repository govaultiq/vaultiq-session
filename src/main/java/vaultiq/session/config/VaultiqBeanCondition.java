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
        String[] beanNames = Objects.requireNonNull(context.getBeanFactory())
                .getBeanNamesForType(VaultiqSessionContext.class, false, false);
        if (beanNames.length == 0) {
            return false;
        }

        Map<String, Object> attrs = metadata.getAnnotationAttributes(
                ConditionalOnVaultiqPersistence.class.getName());
        if (attrs == null) {
            return false;
        }
        VaultiqPersistenceMode mode = (VaultiqPersistenceMode) attrs.get("mode");
        ModelType modelType = (ModelType) attrs.get("type");

        VaultiqSessionContext sessionContext =
                context.getBeanFactory().getBean(VaultiqSessionContext.class);

        var cfg = sessionContext.getModelConfig(modelType);
        if (cfg == null)
            return false;

        boolean useCache = cfg.useCache();
        boolean useJpa = cfg.useJpa();
        return switch (mode) {
            case CACHE_ONLY -> useCache && !useJpa;
            case JPA_ONLY -> !useCache && useJpa;
            case JPA_AND_CACHE -> useCache && useJpa;
        };
    }

}