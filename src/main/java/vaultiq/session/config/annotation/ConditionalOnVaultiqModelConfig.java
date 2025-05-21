
package vaultiq.session.config.annotation;

import org.springframework.context.annotation.Conditional;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.config.rules.VaultiqModelConfigShouldMatchCondition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom conditional annotation to enable beans only if the Vaultiq session configuration
 * matches the specified persistence mode and model type.
 * <p>
 * This annotation is part of Vaultiq's conditional bean creation strategy that implements
 * a flexible storage strategy pattern. It allows different service implementations (cache-based
 * or JPA-based) to be selectively activated based on application configuration.
 * </p>
 * <p>
 * The annotation uses Spring's {@link Conditional} mechanism and delegates to
 * {@link VaultiqModelConfigShouldMatchCondition} to determine if the bean should be created
 * based on the configured persistence method for the specified model types.
 * </p>
 * <p>
 * <strong>Example usage:</strong>
 *
 * <pre>
 * // For cache-based implementations:
 * &#64;Service
 * &#64;ConditionalOnVaultiqModelConfig(
 *     method = VaultiqPersistenceMethod.USE_CACHE,
 *     type = {ModelType.SESSION, ModelType.USER_SESSION_MAPPING}
 * )
 * public class VaultiqSessionCacheService {
 *     // Implementation
 * }
 *
 * // For JPA-based implementations:
 * &#64;Service
 * &#64;ConditionalOnVaultiqModelConfig(
 *     method = VaultiqPersistenceMethod.USE_JPA,
 *     type = ModelType.REVOKE
 * )
 * public class RevokedSessionEntityService {
 *     // Implementation
 * }
 * </pre>
 * </p>
 *
 * @see VaultiqModelConfigShouldMatchCondition The condition that evaluates if the bean should be created
 * @see VaultiqPersistenceMethod Enum defining supported persistence strategies
 * @see ModelType Enum defining different model types in the Vaultiq session system
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(VaultiqModelConfigShouldMatchCondition.class)
public @interface ConditionalOnVaultiqModelConfig {

    /**
     * Specifies the persistence method that must be configured for the model type(s)
     * to activate the annotated bean.
     * <p>
     * The persistence method indicates which storage strategy is being used for the
     * specified model types. Common values include:
     * <ul>
     *   <li>{@link VaultiqPersistenceMethod#USE_CACHE} - For in-memory or distributed cache persistence</li>
     *   <li>{@link VaultiqPersistenceMethod#USE_JPA} - For database persistence via JPA</li>
     * </ul>
     * </p>
     *
     * @return the required persistence method configuration
     */
    VaultiqPersistenceMethod method();

    /**
     * Specifies the model types that must be configured with the specified persistence method
     * to activate the annotated bean.
     * <p>
     * Model types represent different data entities in the Vaultiq session management system.
     * Multiple model types can be specified, forming an AND relationship (all specified types
     * must be configured with the specified persistence method).
     * </p>
     * <p>
     * Common model types include:
     * <ul>
     *   <li>{@link ModelType#SESSION} - Primary session objects (active sessions)</li>
     *   <li>{@link ModelType#USER_SESSION_MAPPING} - Mapping between users and their session IDs</li>
     *   <li>{@link ModelType#REVOKE} - List of revoked/blocklisted sessions</li>
     *   <li>{@link ModelType#USER_ACTIVITY_LOGS} - Activity logs for user sessions</li>
     * </ul>
     * </p>
     *
     * @return array of model types that must match the specified persistence method
     */
    ModelType[] type();
}
