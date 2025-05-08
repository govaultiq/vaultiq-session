package vaultiq.session.cache.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.SessionIds;
import vaultiq.session.cache.utility.VaultiqCacheContext;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.core.VaultiqSession;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;
import vaultiq.session.cache.model.VaultiqSessionCacheEntry;

import java.util.*;

import static vaultiq.session.cache.utility.CacheKeyResolver.keyForSession;
import static vaultiq.session.cache.utility.CacheKeyResolver.keyForUserSessionMapping;

@Service
@ConditionalOnProperty(prefix = "vaultiq.session.persistence.cache", name = "enabled", havingValue = "true")
public class VaultiqSessionCacheService {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionCacheService.class);

    private final Cache sessionPoolCache;
    private final Cache userSessionMappingCache;
    private final DeviceFingerprintGenerator fingerprintGenerator;

    public VaultiqSessionCacheService(
            VaultiqSessionProperties props,
            VaultiqCacheContext cacheContext,
            DeviceFingerprintGenerator fingerprintGenerator) {

        var cacheNames = props.getCache().getCacheNames();
        var propertyPrefix = "vaultiq.session.persistence.cache.cache-names";

        this.sessionPoolCache = cacheContext.getCacheMandatory(cacheNames.getSessions(), propertyPrefix + ".sessions");
        this.userSessionMappingCache = cacheContext.getCacheOptional(
                cacheNames.getUserSessionMapping(),
                propertyPrefix + ".user-session-mapping")
                .orFallbackTo(sessionPoolCache, propertyPrefix + ".sessions");

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
            updated.setSessions(sessionIds);
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
        List<String> sessionIds = getUserSessionIds(userId);
        sessionIds.add(sessionEntry.getSessionId());

        SessionIds updated = new SessionIds();
        updated.setSessions(sessionIds);
        userSessionMappingCache.put(keyForUserSessionMapping(userId), updated);
    }

    public List<String> getUserSessionIds(String userId) {
        return Optional.ofNullable(userSessionMappingCache.get(keyForUserSessionMapping(userId), SessionIds.class))
                .map(SessionIds::getSessions)
                .orElseGet(ArrayList::new);
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
        var sessionIds = sessions.stream().map(VaultiqSession::getSessionId).toList();
        SessionIds ids = new SessionIds();
        ids.setSessions(sessionIds);
        userSessionMappingCache.put(keyForUserSessionMapping(userId), ids);
    }
}
