package vaultiq.session.redis.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vaultiq.session.redis.service.VaultiqSessionManagerViaRedis;

@Configuration
public class RedisSupportAutoConfiguration {

    @Bean
    @ConditionalOnBean(name = "vaultiqCacheManager")
    @ConditionalOnProperty(prefix = "vaultiq.session.persistence.via-redis", name = "allow-inflight-cache-management", havingValue = "true")
    VaultiqSessionManagerViaRedis vaultiqSessionManagerViaRedis(@Qualifier("vaultiqCacheManager") CacheManager cacheManager) {
        return new VaultiqSessionManagerViaRedis(cacheManager);
    }

}
