
package vaultiq.session.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Conditional;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.core.VaultiqSessionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom conditional annotation to enable beans only if the Vaultiq session configuration
 * matches the specified mode and model type.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(VaultiqBeanCondition.class)
public @interface ConditionalOnVaultiqPersistence {

    VaultiqPersistenceMode mode();

    ModelType[] type();
}
