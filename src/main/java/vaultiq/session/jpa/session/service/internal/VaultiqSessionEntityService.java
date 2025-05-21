package vaultiq.session.jpa.session.service.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;
import vaultiq.session.jpa.session.model.VaultiqSessionEntity;
import vaultiq.session.jpa.session.repository.VaultiqSessionEntityRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Internal service for managing Vaultiq sessions using JPA persistence.
 * <p>
 * This class provides the core database interaction logic for session operations.
 * It is typically used by {@link vaultiq.session.jpa.session.service.VaultiqSessionManagerViaJpa}
 * and {@link vaultiq.session.jpa.session.service.VaultiqSessionManagerViaJpaCacheEnabled}
 * to perform create, retrieve, delete, list, and count operations directly against the
 * configured JPA data store.
 * </p>
 * <p>
 * This bean is automatically configured by Spring when a {@link VaultiqSessionEntityRepository}
 * is available and the persistence configuration matches {@link VaultiqPersistenceMethod#USE_JPA}
 * for {@link ModelType#SESSION} and {@link ModelType#USER_SESSION_MAPPING}, as defined
 * by {@link ConditionalOnVaultiqModelConfig}.
 * </p>
 *
 * @see VaultiqSessionEntityRepository
 * @see DeviceFingerprintGenerator
 * @see VaultiqSession
 * @see VaultiqSessionEntity
 * @see ConditionalOnVaultiqModelConfig
 * @see VaultiqPersistenceMethod#USE_JPA
 * @see ModelType#SESSION
 * @see ModelType#USER_SESSION_MAPPING
 */
@Service
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_JPA, type = {ModelType.SESSION, ModelType.USER_SESSION_MAPPING})
// Activate only when JPA is configured for sessions
public class VaultiqSessionEntityService {
    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionEntityService.class);

    private final VaultiqSessionEntityRepository sessionRepository;
    private final DeviceFingerprintGenerator fingerprintGenerator;

    /**
     * Constructs a new {@code VaultiqSessionEntityService} with the required dependencies.
     *
     * @param sessionRepository    The JPA repository for Vaultiq sessions.
     * @param fingerprintGenerator The generator for creating device fingerprints from requests.
     */
    public VaultiqSessionEntityService(
            VaultiqSessionEntityRepository sessionRepository,
            DeviceFingerprintGenerator fingerprintGenerator) {
        this.sessionRepository = sessionRepository;
        this.fingerprintGenerator = fingerprintGenerator;
    }

    /**
     * Creates and persists a new Vaultiq session in the database.
     * <p>
     * Generates a device fingerprint from the request, creates a new JPA entity,
     * saves it using the repository, and maps the saved entity to a {@link VaultiqSession} DTO.
     * </p>
     *
     * @param userId  The unique identifier of the user for the new session.
     * @param request The incoming {@link HttpServletRequest} to generate the device fingerprint from.
     * @return The newly created and persisted {@link VaultiqSession} DTO.
     */
    public VaultiqSession create(String userId, HttpServletRequest request) {
        log.debug("Creating session for user '{}'.", userId);

        // Generate device fingerprint from the request.
        String deviceFingerPrint = fingerprintGenerator.generateFingerprint(request);
        Instant now = Instant.now();

        // Create a new JPA entity.
        VaultiqSessionEntity entity = new VaultiqSessionEntity();
        entity.setUserId(userId);
        entity.setDeviceFingerPrint(deviceFingerPrint);
        entity.setCreatedAt(now);
        // isBlocked and blockedAt will default to false/null in the entity.

        // Save the entity to the database.
        entity = sessionRepository.save(entity);
        log.info("Persisted new session '{}' for user '{}'.", entity.getSessionId(), userId);

        // Map the persisted entity back to the client-facing DTO.
        return mapToVaultiqSession(entity);
    }

    /**
     * Retrieves a Vaultiq session from the database by its session ID.
     *
     * @param sessionId The unique identifier of the session to retrieve.
     * @return The {@link VaultiqSession} DTO if found, or {@code null} if no session exists with the given ID in the database.
     */
    public VaultiqSession get(String sessionId) {
        log.debug("Retrieving session '{}'.", sessionId);

        // Find the entity by ID and map it to a VaultiqSession DTO if found.
        VaultiqSession session = sessionRepository.findById(sessionId)
                .map(this::mapToVaultiqSession).orElse(null);

        if (session == null) {
            log.info("Session '{}' not found in database.", sessionId);
        } else {
            log.debug("Session '{}' loaded from database.", sessionId);
        }

        return session;
    }

    /**
     * Deletes a Vaultiq session from the database by its session ID.
     * <p>
     * Finds the session entity by ID and deletes it if present.
     * </p>
     *
     * @param sessionId The unique identifier of the session to delete.
     */
    public void delete(String sessionId) {
        // Find the entity by ID and delete it if present.
        sessionRepository.findById(sessionId).ifPresent(entity -> {
            sessionRepository.delete(entity);
            log.info("Deleted session '{}' for user '{}'.", sessionId, entity.getUserId());
        });
    }

    /**
     * Deletes all Vaultiq sessions from the database by their session IDs.
     * <p>
     * Find all entities by user ID and delete them if present.
     * </p>
     *
     * @param sessionIds The set of session IDs to delete.
     */
    @Transactional
    public void deleteAllSessions(Set<String> sessionIds) {
        sessionRepository.deleteAllById(sessionIds);
        log.info("Deleted {} sessions via JPA. Sessions: {}", sessionIds.size(), sessionIds);
    }

    /**
     * Retrieves all Vaultiq sessions for a specific user from the database.
     *
     * @param userId The unique identifier of the user whose sessions are to be retrieved.
     * @return A {@link List} of {@link VaultiqSession} DTOs for the user. Returns an empty list if no sessions are found.
     */
    public List<VaultiqSession> list(String userId) {
        log.debug("Fetching sessions for user '{}'.", userId);

        // Find all entities by user ID and map them to VaultiqSession DTOs.
        return sessionRepository.findAllByUserId(userId).stream()
                .map(this::mapToVaultiqSession)
                .toList();
    }

    /**
     * Retrieves all active Vaultiq sessions for a specific user from the database.
     *
     * @param userId The unique identifier of the user whose sessions are to be retrieved.
     * @return A {@link List} of {@link VaultiqSession} DTOs for the user. Returns an empty list if no sessions are found.
     */
    public List<VaultiqSession> getActiveSessionsByUser(String userId) {
        log.debug("Fetching active sessions for user '{}'.", userId);

        return sessionRepository.findAllByUserIdAndIsBlocked(userId, false).stream()
                .map(this::mapToVaultiqSession).toList();
    }

    /**
     * Counts the total number of Vaultiq sessions for a specific user in the database.
     *
     * @param userId The unique identifier of the user whose sessions are to be counted.
     * @return The total number of sessions for the user in the database.
     */
    public int count(String userId) {
        log.debug("Counting sessions for user '{}'.", userId);
        return sessionRepository.countByUserId(userId);
    }

    /**
     * Maps a JPA {@link VaultiqSessionEntity} entity to a client-facing {@link VaultiqSession} DTO.
     *
     * @param entity The JPA entity to map.
     * @return The corresponding {@link VaultiqSession} DTO.
     */
    private VaultiqSession mapToVaultiqSession(VaultiqSessionEntity entity) {
        return VaultiqSession.builder()
                .sessionId(entity.getSessionId())
                .userId(entity.getUserId())
                .deviceFingerPrint(entity.getDeviceFingerPrint())
                .createdAt(entity.getCreatedAt())
                .isBlocked(entity.isBlocked())
                .blockedAt(entity.getBlockedAt())
                .build();
    }
}
