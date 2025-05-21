package vaultiq.session.config.annotation;

import org.springframework.context.annotation.Conditional;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.config.rules.VaultiqPersistenceModeCondition;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.context.VaultiqSessionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Conditional} annotation that enables a bean only when the Vaultiq session
 * configuration matches the specified persistence mode for at least one of the
 * provided model types.
 * <p>
 * This annotation leverages Spring's {@link Conditional} mechanism and the
 * {@link VaultiqPersistenceModeCondition} to evaluate the condition based on the
 * application's configured {@link VaultiqPersistenceMode} for specific
 * {@link ModelType}s within the {@link VaultiqSessionContext}.
 * </p>
 * <p>
 * It provides a flexible way to activate different bean implementations (e.g.,
 * cache-based vs. JPA-based services) depending on how the session data models
 * are configured for persistence.
 * </p>
 *
 * @see VaultiqPersistenceModeCondition The {@link org.springframework.context.annotation.Condition} implementation that evaluates the logic.
 * @see VaultiqPersistenceMode Defines the possible persistence modes.
 * @see ModelType Defines the different types of session data models.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(VaultiqPersistenceModeCondition.class)
public @interface ConditionalOnVaultiqPersistence {

    /**
     * The required {@link VaultiqPersistenceMode} that must be configured
     * for at least one of the specified {@link ModelType}s to enable the bean.
     * <p>
     * This specifies the expected storage strategy (e.g., cache only, JPA only,
     * or both) that the session model should be using.
     * </p>
     *
     * @return the required persistence mode.
     */
    VaultiqPersistenceMode mode();

    /**
     * The {@link ModelType}(s) for which the configuration must match the specified
     * {@link #mode()}.
     * <p>
     * The condition evaluates to true if the configuration for *at least one* of
     * the model types listed in this array matches the specified {@link #mode()}.
     * </p>
     *
     * @return an array of model types to check against the required mode.
     */
    ModelType[] type();
}