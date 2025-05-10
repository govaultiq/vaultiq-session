package vaultiq.session.cache.service.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.cache.model.SessionIds;
import vaultiq.session.cache.util.VaultiqCacheContext;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.core.util.VaultiqSessionContext;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;
import vaultiq.session.cache.model.VaultiqSessionCacheEntry;

import java.util.*;
import java.util.stream.Collectors;

import static vaultiq.session.cache.util.CacheKeyResolver.keyForSession;
import static vaultiq.session.cache.util.CacheKeyResolver.keyForUserSessionMapping;

@Service
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = {ModelType.SESSION, ModelType.USER_SESSION_MAPPING})
public class VaultiqSessionCacheService {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionCacheService.class);

    private final Cache sessionPoolCache;
    private final Cache userSessionMappingCache;
    private final DeviceFingerprintGenerator fingerprintGenerator;

    public VaultiqSessionCacheService(
            VaultiqSessionContext context,
            VaultiqCacheContext cacheContext,
            DeviceFingerprintGenerator fingerprintGenerator) {

        this.sessionPoolCache = cacheContext.getCacheMandatory(context.getModelConfig(ModelType.SESSION).cacheName(), ModelType.SESSION);
        this.userSessionMappingCache = cacheContext.getCacheOptional(
                        context.getModelConfig(ModelType.USER_SESSION_MAPPING).cacheName(),
                        ModelType.USER_SESSION_MAPPING)
                .orFallbackTo(sessionPoolCache, ModelType.SESSION);

        this.fingerprintGenerator = fingerprintGenerator;
    }

    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        String fingerprint = fingerprintGenerator.generateFingerprint(request);
        VaultiqSessionCacheEntry sessionEntry = VaultiqSessionCacheEntry.create(userId, fingerprint);
        log.info("Creating session for userId={}, deviceFingerprint={}", userId, fingerprint);

        sessionPoolCache.put(keyForSession(sessionEntry.getSessionId()), sessionEntry);
        mapNewSessionIdToUser(userId, sessionEntry);

        log.debug("Session added to pool and cached for userId={}, sessionId={}", userId, sessionEntry.getSessionId());
        return toVaultiqSession(sessionEntry);
    }

    public void cacheSession(VaultiqSession vaultiqSession) {
        log.info("Caching session with sessionId={}", vaultiqSession.getSessionId());
        VaultiqSessionCacheEntry cacheEntry = VaultiqSessionCacheEntry.copy(vaultiqSession);
        sessionPoolCache.put(keyForSession(vaultiqSession.getSessionId()), cacheEntry);
    }

    public VaultiqSession getSession(String sessionId) {
        var session = Optional.ofNullable(sessionPoolCache.get(keyForSession(sessionId), VaultiqSessionCacheEntry.class))
                .map(this::toVaultiqSession)
                .orElse(null);

        log.debug("Retrieved sessionId={} from session pool, found={}", sessionId, session != null);

        return session;
    }

    public void deleteSession(String sessionId) {
        log.debug("Deleting session with sessionId={}", sessionId);
        var session = getSession(sessionId);

        if (session != null) {
            sessionPoolCache.evict(keyForSession(sessionId));
            var sessionIds = getUserSessionIds(session.getUserId());
            sessionIds.remove(sessionId);

            SessionIds updated = new SessionIds();
            updated.setSessionIds(sessionIds);
            userSessionMappingCache.put(keyForUserSessionMapping(session.getUserId()), updated);
        }
    }

    public List<VaultiqSession> getSessionsByUser(String userId) {
        var sessionIds = getUserSessionIds(userId);
        var sessions = sessionIds.stream()
                .map(this::getSession)
                .filter(Objects::nonNull)
                .toList();

        log.debug("Fetched {} sessions for userId={}", sessions.size(), userId);
        return sessions;
    }

    public int totalUserSessions(String userId) {
        return getUserSessionIds(userId).size();
    }

    private void mapNewSessionIdToUser(String userId, VaultiqSessionCacheEntry sessionEntry) {
        var sessionIds = getUserSessionIds(userId);
        sessionIds.add(sessionEntry.getSessionId());

        SessionIds updated = new SessionIds();
        updated.setSessionIds(sessionIds);
        userSessionMappingCache.put(keyForUserSessionMapping(userId), updated);
    }

    public Set<String> getUserSessionIds(String userId) {
        return Optional.ofNullable(userSessionMappingCache.get(keyForUserSessionMapping(userId), SessionIds.class))
                .map(SessionIds::getSessionIds)
                .orElseGet(HashSet::new);
    }

    private VaultiqSession toVaultiqSession(VaultiqSessionCacheEntry source) {
        return VaultiqSession.builder()
                .sessionId(source.getSessionId())
                .userId(source.getUserId())
                .deviceFingerPrint(source.getDeviceFingerPrint())
                .createdAt(source.getCreatedAt())
                .build();
    }

    public void cacheUserSessions(String userId, List<VaultiqSession> sessions) {
        sessions.forEach(this::cacheSession);
        var sessionIds = sessions.stream().map(VaultiqSession::getSessionId).collect(Collectors.toSet());
        SessionIds ids = new SessionIds();
        ids.setSessionIds(sessionIds);
        userSessionMappingCache.put(keyForUserSessionMapping(userId), ids);
    }
}
