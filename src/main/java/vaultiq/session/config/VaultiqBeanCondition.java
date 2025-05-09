
package vaultiq.session.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.core.util.VaultiqSessionContext;

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

        Object typesObj = attrs.get("type");
        ModelType[] modelTypes;
        if (typesObj instanceof ModelType[]) {
            modelTypes = (ModelType[]) typesObj;
        } else if (typesObj instanceof ModelType) {
            modelTypes = new ModelType[] {(ModelType) typesObj};
        } else {
            return false;
        }

        VaultiqSessionContext sessionContext =
                context.getBeanFactory().getBean(VaultiqSessionContext.class);

        for (ModelType modelType : modelTypes) {
            var cfg = sessionContext.getModelConfig(modelType);
            if (cfg == null)
                continue;

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

        // None matched
        return false;
    }
}
