package vaultiq.session.config.annotation;

import org.springframework.context.annotation.Conditional;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.config.rules.VaultiqPersistenceRequirementByAny;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Conditional} annotation that enables a bean only if the Vaultiq session
 * configuration indicates that a specific persistence method (JPA or Cache) is
 * enabled for *any* configured model type.
 * <p>
 * This annotation is evaluated by the {@link VaultiqPersistenceRequirementByAny}
 * condition. It checks the overall persistence status across all model types,
 * as determined by the library's resolved configuration.
 * </p>
 * <p>
 * Use this annotation to conditionally register beans that require the presence
 * of either JPA or Cache persistence within the Vaultiq session setup,
 * regardless of which specific model types are configured to use it.
 * </p>
 *
 * @see VaultiqPersistenceRequirementByAny The condition that evaluates this annotation.
 * @see VaultiqPersistenceMethod Defines the possible persistence methods (USE_JPA, USE_CACHE).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(VaultiqPersistenceRequirementByAny.class)
public @interface ConditionalOnVaultiqPersistenceRequirement {
    /**
     * Specifies the {@link VaultiqPersistenceMethod} that must be enabled for
     * *any* configured model type for the annotated bean to be registered.
     * <p>
     * The condition checks if the overall configuration includes this method
     * for at least one model type.
     * </p>
     *
     * @return the required persistence method.
     */
    VaultiqPersistenceMethod value();
}
