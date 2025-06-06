package vaultiq.session.config.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.MethodMetadata;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistenceRequirement;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.context.VaultiqSessionContext;
import vaultiq.session.context.VaultiqSessionContextHolder;

import java.util.Map;

/**
 * Spring {@link Condition} implementation that evaluates whether a bean should be enabled
 * based on whether *any* configured Vaultiq session model type uses a specific
 * persistence method (JPA or Cache).
 * <p>
 * This condition is intended for use with annotations that specify a required
 * {@link VaultiqPersistenceMethod} (e.g., a variant of {@link ConditionalOnVaultiqPersistence}
 * or a similar custom annotation). It checks the overall persistence status
 * determined by {@link VaultiqSessionContext#isUsingJpa()} and
 * {@link VaultiqSessionContext#isUsingCache()}.
 * </p>
 * <p>
 * The condition returns {@code true} if the required persistence method is enabled
 * for at least one model type in the resolved configuration; otherwise, it returns
 * {@code false}.
 * </p>
 *
 * @see VaultiqPersistenceMethod Defines the possible persistence methods (USE_JPA, USE_CACHE).
 * @see VaultiqSessionContext Provides access to the aggregate persistence status.
 * @see org.springframework.context.annotation.Conditional
 */
public class VaultiqPersistenceRequirementByAny implements Condition {
    private final static Logger log = LoggerFactory.getLogger(VaultiqPersistenceRequirementByAny.class);

    /**
     * Determines if the condition matches and the bean should be enabled.
     * <p>
     * This method is invoked by Spring's conditional mechanism. It checks
     * if the {@link VaultiqSessionContext} bean is available and retrieves
     * the required {@link VaultiqPersistenceMethod} from the annotation's
     * attributes (assumed to be under the "value" key). It then compares
     * this required method against the overall {@code isUsingJpa()} and
     * {@code isUsingCache()} status reported by the {@link VaultiqSessionContext}.
     * </p>
     *
     * @param context  The condition context, providing access to the bean factory, environment, etc.
     * @param metadata The annotated type metadata, providing access to annotations on the bean class or method.
     * @return {@code true} if the required persistence method is active for
     * any model type, {@code false} otherwise.
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

        Map<String, Object> attrs = metadata.getAnnotationAttributes(
                ConditionalOnVaultiqPersistenceRequirement.class.getName());
        if (attrs == null) return false;
        String className = (metadata instanceof ClassMetadata cm) ? cm.getClassName()
                : (metadata instanceof MethodMetadata mm) ? mm.getDeclaringClassName()
                : "Unknown";

        var method = (VaultiqPersistenceMethod) attrs.get("value");

        log.debug("Validating Condition - if persistence method '{}' required; Triggered by: '{}'", method, className);

        VaultiqSessionContext sessionContext = VaultiqSessionContextHolder.getContext();

        var result = switch (method) {
            case USE_JPA -> sessionContext.isUsingJpa();
            case USE_CACHE -> sessionContext.isUsingCache();
        };
        log.debug("Condition result: {}", result);
        return result;
    }
}