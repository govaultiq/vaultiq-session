package vaultiq.session.cache.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.core.VaultiqSession;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;
import vaultiq.session.cache.model.VaultiqSessionCacheEntry;

import java.io.Serializable;
import java.util.*;

@Service
@ConditionalOnProperty(prefix = "vaultiq.session.persistence.cache", name = "enabled", havingValue = "true")
public class VaultiqSessionCacheService {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionCacheService.class);

    private final Cache sessionPoolCache;
    private final Cache userSessionMappingCache;
    private final DeviceFingerprintGenerator fingerprintGenerator;

    public VaultiqSessionCacheService(
            VaultiqSessionProperties props,
            Map<String, CacheManager> cacheManagers,
            DeviceFingerprintGenerator fingerprintGenerator) {

        String configuredCacheManagerName = props.getCache().getManager();
        if (configuredCacheManagerName == null || !cacheManagers.containsKey(configuredCacheManagerName)) {
            log.error("CacheManager `{}` not found, isConfigured: {}", configuredCacheManagerName,
                    configuredCacheManagerName != null && !configuredCacheManagerName.isBlank());
            throw new IllegalStateException("Required CacheManager '" + configuredCacheManagerName + "' not found.");
        }

        CacheManager cacheManager = cacheManagers.get(configuredCacheManagerName);
        VaultiqSessionProperties.CacheNames cacheNames = props.getCache().getCacheNames();

        this.sessionPoolCache = getRequiredCache(cacheManager, cacheNames.getSessions());
        this.userSessionMappingCache = getOptionalCache(cacheManager, cacheNames.getUserSessionMapping(), "user-session-mapping");

        this.fingerprintGenerator = fingerprintGenerator;
    }

    private Cache getRequiredCache(CacheManager cacheManager, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Missing cache name for type 'session-pool'");
        }
        Cache cache = cacheManager.getCache(name);
        if (cache == null) {
            throw new IllegalStateException("Cache named '" + name + "' not found in configured CacheManager.");
        }
        return cache;
    }

    private Cache getOptionalCache(CacheManager manager, String name, String label) {
        if (name == null || name.isBlank()) {
            log.warn("No cache name configured for '{}', falling back to session-pool.", label);
            return sessionPoolCache;
        }
        Cache cache = manager.getCache(name);
        if (cache == null) {
            log.warn("Cache '{}' not found in CacheManager, falling back to session-pool.", name);
            return sessionPoolCache;
        }
        return cache;
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

    private static class SessionIds implements Serializable {
        private List<String> sessions = new ArrayList<>();

        public List<String> getSessions() {
            return sessions;
        }

        public void setSessions(List<String> sessions) {
            this.sessions = sessions != null ? sessions : new ArrayList<>();
        }
    }

    private static String keyForSession(String sessionId) {
        return "session-pool-" + sessionId;
    }

    private static String keyForUserSessionMapping(String userId) {
        return "user-sessions-" + userId;
    }

    private static String keyForLastActiveTimestamp(String userId) {
        return "last-active-" + userId;
    }
}
