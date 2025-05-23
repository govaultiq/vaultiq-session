package vaultiq.session.config.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;
import vaultiq.session.cache.util.CacheType;
import vaultiq.session.cache.util.VaultiqCacheContext;
import vaultiq.session.config.annotation.ConditionalOnCache;

import java.util.Objects;

/**
 * A Spring {@link Condition} that checks if a specific cache, identified by its {@link CacheType},
 * exists in the application's {@link VaultiqCacheContext}.
 * <p>
 * This condition is typically used with the {@link ConditionalOnCache @ConditionalOnCache} annotation
 * to conditionally enable or disable bean creation based on the presence of a required cache.
 * </p>
 */
public class CacheExistsCondition implements Condition {
    private static final Logger log = LoggerFactory.getLogger(CacheExistsCondition.class);

    /**
     * Determines if the condition matches, i.e., if the specified cache exists.
     * <p>
     * It extracts the {@link CacheType} from the {@link ConditionalOnCache @ConditionalOnCache}
     * annotation on the annotated element and checks if {@link VaultiqCacheContext}
     * contains a cache for that type.
     * </p>
     * @param context The current condition context.
     * @param metadata The metadata of the annotated class or method.
     * @return {@code true} if the specified cache exists, {@code false} otherwise.
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var attrs = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(ConditionalOnCache.class.getName(), false)
        );
        if (attrs == null) return false;

        CacheType cacheType = attrs.getEnum("value");

        var bf = context.getBeanFactory();
        var cacheCtx = Objects.requireNonNull(bf).getBeanProvider(VaultiqCacheContext.class)
                .getIfAvailable();

        boolean present = cacheCtx != null && cacheCtx.getCache(cacheType) != null;
        if (!present) {
            log.info("Cache '{}' not found; skipping beans that depend on it.", cacheType.alias());
        } else {
            log.debug("Cache '{}' found; enabling beans that depend on it.", cacheType.alias());
        }
        return present;
    }
}