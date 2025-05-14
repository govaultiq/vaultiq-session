package vaultiq.session.config.annotation;

import org.springframework.context.annotation.Conditional;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.config.rules.VaultiqPersistenceModeCondition;
import vaultiq.session.config.rules.VaultiqPersistenceRequirementByAny;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(VaultiqPersistenceRequirementByAny.class)
public @interface ConditionalOnVaultiqPersistenceRequirement {
    VaultiqPersistenceMethod value();
}
