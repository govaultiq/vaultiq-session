package vaultiq.session.config.annotation;

import org.springframework.context.annotation.Conditional;
import vaultiq.session.cache.util.CacheType;
import vaultiq.session.config.rules.CacheExistsCondition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that conditionally enables a bean or configuration class
 * only if a specific cache type exists in the application context.
 * <p>
 * This annotation uses {@link CacheExistsCondition} to perform the check at application startup.
 * </p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(CacheExistsCondition.class)
public @interface ConditionalOnCache {
    /**
     * The {@link CacheType} that must be present for the annotated component
     * to be registered.
     *
     * @return The required {@link CacheType}.
     */
    CacheType value();
}