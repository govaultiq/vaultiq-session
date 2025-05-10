
package vaultiq.session.config.rules;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.core.util.VaultiqSessionContext;

import java.util.Map;

/**
 * Condition for enabling beans based on Vaultiq persistence configuration and model type.
 * Simplified to leverage enum array annotation semantics and perform direct casts.
 */
public class VaultiqPersistenceModeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var beanFactory = context.getBeanFactory();
        if (beanFactory == null || beanFactory.getBeanNamesForType(VaultiqSessionContext.class, false, false).length == 0) {
            return false;
        }

        Map<String, Object> attrs = metadata.getAnnotationAttributes(
                ConditionalOnVaultiqPersistence.class.getName());
        if (attrs == null) return false;

        VaultiqPersistenceMode mode = (VaultiqPersistenceMode) attrs.get("mode");
        ModelType[] modelTypes = (ModelType[]) attrs.get("type");

        VaultiqSessionContext sessionContext = beanFactory.getBean(VaultiqSessionContext.class);

        for (ModelType modelType : modelTypes) {
            var cfg = sessionContext.getModelConfig(modelType);
            if (cfg == null) continue;

            boolean useCache = cfg.useCache();
            boolean useJpa = cfg.useJpa();
            boolean matches = switch (mode) {
                case CACHE_ONLY -> useCache && !useJpa;
                case JPA_ONLY -> !useCache && useJpa;
                case JPA_AND_CACHE -> useCache && useJpa;
            };

            if (matches) {
                return true;
            }
        }
        return false;
    }
}
