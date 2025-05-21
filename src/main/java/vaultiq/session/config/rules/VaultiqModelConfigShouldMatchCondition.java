
package vaultiq.session.config.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.context.VaultiqSessionContextHolder;
import vaultiq.session.core.model.VaultiqModelConfig;
import vaultiq.session.context.VaultiqSessionContext;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Implements Spring's {@link Condition} interface to provide the conditional logic
 * for the {@link ConditionalOnVaultiqModelConfig} annotation.
 * <p>
 * This class determines whether a bean annotated with {@link ConditionalOnVaultiqModelConfig}
 * should be created based on the current application configuration. The decision is made
 * by checking if the configured persistence methods for the specified model types match
 * what is required by the annotation.
 * </p>
 * <p>
 * The condition works by:
 * <ol>
 *   <li>Extracting the required persistence method and model types from the annotation</li>
 *   <li>Retrieving the current configuration for those model types from {@link VaultiqSessionContext}</li>
 *   <li>Determining if the configured persistence methods match what is required</li>
 * </ol>
 * </p>
 * <p>
 * For example, if a bean is annotated with:
 * <pre>
 * &#64;ConditionalOnVaultiqModelConfig(
 *     method = VaultiqPersistenceMethod.USE_CACHE,
 *     type = {ModelType.SESSION, ModelType.USER_SESSION_MAPPING}
 * )
 * </pre>
 * This condition will only return true if both the SESSION and USER_SESSION_MAPPING models
 * are configured to use cache-based persistence in the application configuration.
 * </p>
 *
 * @see ConditionalOnVaultiqModelConfig The annotation that uses this condition
 * @see VaultiqSessionContext The context that provides the current configuration
 * @see VaultiqModelConfig The configuration for each model type
 */
public class VaultiqModelConfigShouldMatchCondition implements Condition {

    private final static Logger log = LoggerFactory.getLogger(VaultiqModelConfigShouldMatchCondition.class);
    /**
     * Evaluates whether the condition is satisfied for the given context.
     * <p>
     * This method implements the core condition logic that determines if a bean
     * annotated with {@link ConditionalOnVaultiqModelConfig} should be created.
     * </p>
     * <p>
     * The evaluation process:
     * <ol>
     *   <li>Verifies that the required bean factory and {@link VaultiqSessionContext} exist</li>
     *   <li>Extracts the required persistence method and model types from the annotation</li>
     *   <li>For each specified model type, checks if its configured persistence method matches the requirement</li>
     *   <li>Returns true if the configuration matches for all specified model types</li>
     * </ol>
     * </p>
     *
     * @param context  The Spring condition context providing access to the bean factory
     * @param metadata The metadata for the annotated class or method, containing annotation attributes
     * @return true if the bean should be created based on the current configuration, false otherwise
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

        // Extract annotation attributes
        Map<String, Object> attrs = metadata.getAnnotationAttributes(
                ConditionalOnVaultiqModelConfig.class.getName());
        if (attrs == null) return false;

        // Get the required persistence method and model types from the annotation
        VaultiqPersistenceMethod method = (VaultiqPersistenceMethod) attrs.get("method");
        ModelType[] modelTypes = (ModelType[]) attrs.get("type");

        log.debug("Validating condition for persistence method: {}, and modelTypes: {}", method, modelTypes);
        // Get the VaultiqSessionContext to access current configuration
        VaultiqSessionContext sessionContext = VaultiqSessionContextHolder.getContext();

        // Check if all specified model types match the required persistence method
        var result = Arrays.stream(modelTypes)
                .map(sessionContext::getModelConfig)
                .filter(Objects::nonNull)                       // Filter out any null configs
                .anyMatch(cfg -> switch (method) {              // Check if method matches requirement
                    case USE_CACHE -> cfg.useCache();           // For cache-based persistence
                    case USE_JPA -> cfg.useJpa();               // For JPA-based persistence
                });
        log.debug("Condition result: {}", result);
        return result;
    }
}
