package vaultiq.session.config.annotation;

import org.springframework.context.annotation.Conditional;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.VaultiqPersistenceMethod;
import vaultiq.session.config.rules.VaultiqModelConfigShouldMatchCondition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom conditional annotation to enable beans only if the Vaultiq session configuration
 * matches the specified persistence mode and model type.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(VaultiqModelConfigShouldMatchCondition.class)
public @interface ConditionalOnVaultiqModelConfig {
    VaultiqPersistenceMethod method();
    ModelType[] type();
}
