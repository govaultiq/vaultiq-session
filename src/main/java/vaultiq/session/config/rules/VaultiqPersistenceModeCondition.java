package vaultiq.session.config.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.core.util.VaultiqSessionContext;

import java.util.Map;

/**
 * Spring {@link Condition} implementation that evaluates whether a bean should be enabled
 * based on the configured persistence mode for specific Vaultiq session model types.
 * <p>
 * This condition is used by the {@link ConditionalOnVaultiqPersistence} annotation.
 * It checks the application's configuration via {@link VaultiqSessionContext} to determine
 * if the persistence settings (cache enabled, JPA enabled) for any of the specified
 * {@link ModelType}s match the required {@link VaultiqPersistenceMode}.
 * </p>
 * <p>
 * The condition returns {@code true} if the configured persistence mode for at least one
 * of the model types listed in the {@code @ConditionalOnVaultiqPersistence} annotation
 * matches the required mode specified in the annotation. Otherwise, it returns {@code false}.
 * </p>
 *
 * @see ConditionalOnVaultiqPersistence The annotation that uses this condition.
 * @see VaultiqPersistenceMode Defines the possible persistence modes (CACHE_ONLY, JPA_ONLY, JPA_AND_CACHE).
 * @see ModelType Defines the different types of session data models.
 * @see VaultiqSessionContext Provides access to the configured model persistence settings.
 */
public class VaultiqPersistenceModeCondition implements Condition {
    private final static Logger log = LoggerFactory.getLogger(VaultiqPersistenceModeCondition.class);

    /**
     * Determines if the condition matches and the bean should be enabled.
     * <p>
     * This method is invoked by Spring's conditional mechanism. It retrieves the
     * {@link ConditionalOnVaultiqPersistence} annotation attributes from the
     * {@link AnnotatedTypeMetadata} and checks if the application's configured
     * persistence mode for any of the specified {@link ModelType}s matches the
     * required {@link VaultiqPersistenceMode}.
     * </p>
     *
     * @param context  The condition context, providing access to the bean factory, environment, etc.
     * @param metadata The annotated type metadata, providing access to annotations on the bean class or method.
     * @return {@code true} if the condition matches (i.e., the configured persistence mode
     * for at least one specified model type matches the required mode), {@code false} otherwise.
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var beanFactory = context.getBeanFactory();
        // Check if bean factory is available and VaultiqSessionContext bean exists.
        if (beanFactory == null || beanFactory.getBeanNamesForType(VaultiqSessionContext.class, false, false).length == 0) {
            log.error("VaultiqSessionContext not found in the bean factory.");
            return false;
        }

        // Get the attributes of the ConditionalOnVaultiqPersistence annotation.
        Map<String, Object> attrs = metadata.getAnnotationAttributes(
                ConditionalOnVaultiqPersistence.class.getName());
        if (attrs == null) return false;

        // Extract the required persistence mode and model types from annotation attributes.
        VaultiqPersistenceMode mode = (VaultiqPersistenceMode) attrs.get("mode");
        ModelType[] modelTypes = (ModelType[]) attrs.get("type");

        // Get the VaultiqSessionContext bean.
        VaultiqSessionContext sessionContext = beanFactory.getBean(VaultiqSessionContext.class);

        // Iterate through the specified model types.
        for (ModelType modelType : modelTypes) {
            // Get the persistence configuration for the current model type.
            var cfg = sessionContext.getModelConfig(modelType);
            // Skip if configuration for this model type is not found.
            if (cfg == null) continue;

            // Check persistence settings for the model type.
            boolean useCache = cfg.useCache();
            boolean useJpa = cfg.useJpa();

            // Determine if the configured settings match the required persistence mode.
            boolean matches = switch (mode) {
                case CACHE_ONLY -> useCache && !useJpa;
                case JPA_ONLY -> !useCache && useJpa;
                case JPA_AND_CACHE -> useCache && useJpa;
            };

            // If a match is found for any model type, the condition is met.
            if (matches) {
                return true;
            }
        }
        // No match found for any specified model type.
        return false;
    }
}
