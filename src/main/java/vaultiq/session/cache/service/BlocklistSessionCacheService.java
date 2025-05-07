package vaultiq.session.cache.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.SessionIds;
import vaultiq.session.cache.utility.VaultiqCacheContext;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

import static vaultiq.session.cache.utility.CacheKeyResolver.keyForBlacklist;

@Service
@ConditionalOnBean(VaultiqCacheContext.class)
public class BlocklistSessionCacheService {
    private static final Logger log = LoggerFactory.getLogger(BlocklistSessionCacheService.class);

    private final VaultiqSessionCacheService vaultiqSessionCacheService;
    private final Cache blocklistCache;

    public BlocklistSessionCacheService(
            VaultiqCacheContext cacheContainer,
            VaultiqSessionCacheService vaultiqSessionCacheService) {
        this.vaultiqSessionCacheService = vaultiqSessionCacheService;
        this.blocklistCache = cacheContainer.getBlocklistCache();
    }

    public void blocklistSession(String sessionId) {
        var session = vaultiqSessionCacheService.getSession(sessionId);
        Optional.ofNullable(session)
                .ifPresent(s -> {
                    var sessionIds = getBlockedSessions(s.getUserId());
                    sessionIds.getSessions().add(sessionId);

                    blocklistCache.put(keyForBlacklist(s.getUserId()), sessionIds);
                    vaultiqSessionCacheService.deleteSession(sessionId);

                    log.debug("Session with sessionId={} blocked", sessionId);
                });
    }

    public SessionIds getBlockedSessions(String userId) {
        return blocklistCache.get(keyForBlacklist(userId), SessionIds.class);
    }

}
