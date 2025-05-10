
package vaultiq.session.config.rules;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.VaultiqPersistenceMethod;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.core.util.VaultiqSessionContext;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class VaultiqModelConfigShouldMatchCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var beanFactory = context.getBeanFactory();
        if (beanFactory == null || beanFactory.getBeanNamesForType(VaultiqSessionContext.class, false, false).length == 0) {
            return false;
        }

        Map<String, Object> attrs = metadata.getAnnotationAttributes(
                ConditionalOnVaultiqModelConfig.class.getName());
        if (attrs == null) return false;

        VaultiqPersistenceMethod method = (VaultiqPersistenceMethod) attrs.get("method");
        ModelType[] modelTypes = (ModelType[]) attrs.get("type");

        VaultiqSessionContext sessionContext = beanFactory.getBean(VaultiqSessionContext.class);

        return Arrays.stream(modelTypes)
                .map(sessionContext::getModelConfig)
                .filter(Objects::nonNull)
                .anyMatch(cfg -> switch (method) {
                    case USE_CACHE -> cfg.useCache();
                    case USE_JPA -> cfg.useJpa();
                });
    }
}
