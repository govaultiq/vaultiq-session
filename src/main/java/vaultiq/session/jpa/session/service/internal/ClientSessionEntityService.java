package vaultiq.session.jpa.session.service.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vaultiq.session.core.util.BatchProcessingHelper;
import vaultiq.session.core.util.SessionAttributor;
import vaultiq.session.model.ModelType;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.model.ClientSession;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;
import vaultiq.session.jpa.session.model.ClientSessionEntity;
import vaultiq.session.jpa.session.repository.ClientSessionEntityRepository;
import vaultiq.session.jpa.session.service.SessionManagerViaJpa;
import vaultiq.session.jpa.session.service.SessionManagerViaJpaCacheEnabled;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Internal service for managing Vaultiq sessions using JPA persistence.
 * <p>
 * This class provides the core database interaction logic for session operations.
 * It is typically used by {@link SessionManagerViaJpa}
 * and {@link SessionManagerViaJpaCacheEnabled}
 * to perform create, retrieve, delete, list, and count operations directly against the
 * configured JPA data store.
 * </p>
 * <p>
 * This bean is automatically configured by Spring when a {@link ClientSessionEntityRepository}
 * is available and the persistence configuration matches {@link VaultiqPersistenceMethod#USE_JPA}
 * for {@link ModelType#SESSION}, as defined
 * by {@link ConditionalOnVaultiqModelConfig}.
 * </p>
 *
 * @see ClientSessionEntityRepository
 * @see DeviceFingerprintGenerator
 * @see ClientSession
 * @see ClientSessionEntity
 * @see ConditionalOnVaultiqModelConfig
 * @see VaultiqPersistenceMethod#USE_JPA
 * @see ModelType#SESSION
 */
@Service
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_JPA, type = ModelType.SESSION)
// Activate only when JPA is configured for sessions
public class ClientSessionEntityService {
    private static final Logger log = LoggerFactory.getLogger(ClientSessionEntityService.class);

    private final ClientSessionEntityRepository sessionRepository;
    private final DeviceFingerprintGenerator fingerprintGenerator;

    /**
     * Constructs a new {@code ClientSessionEntityService} with the required dependencies.
     *
     * @param sessionRepository    The JPA repository for Vaultiq sessions.
     * @param fingerprintGenerator The generator for creating device fingerprints from requests.
     */
    public ClientSessionEntityService(
            ClientSessionEntityRepository sessionRepository,
            DeviceFingerprintGenerator fingerprintGenerator) {
        this.sessionRepository = sessionRepository;
        this.fingerprintGenerator = fingerprintGenerator;
    }

    /**
     * Creates and persists a new Vaultiq session in the database.
     * <p>
     * Generates a device fingerprint from the request, creates a new JPA entity,
     * saves it using the repository, and maps the saved entity to a {@link ClientSession} DTO.
     * </p>
     *
     * @param userId  The unique identifier of the user for the new session.
     * @param request The incoming {@link HttpServletRequest} to generate the device fingerprint from.
     * @return The newly created and persisted {@link ClientSession} DTO.
     */
    public ClientSession create(String userId, HttpServletRequest request) {
        log.debug("Creating session for user '{}'.", userId);

        // Generate device fingerprint from the request.
        String deviceFingerPrint = fingerprintGenerator.generateFingerprint(request);

        // Create a new JPA entity.
        var sessionAttributes = SessionAttributor.forRequest(request);
        ClientSessionEntity entity = ClientSessionEntity.create(userId, deviceFingerPrint, sessionAttributes.deviceName(), sessionAttributes.os(), sessionAttributes.deviceType());

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
     * @return The {@link ClientSession} DTO if found, or {@code null} if no session exists with the given ID in the database.
     */
    public ClientSession get(String sessionId) {
        log.debug("Retrieving session '{}'.", sessionId);

        // Find the entity by ID and map it to a ClientSession DTO if found.
        ClientSession session = sessionRepository.findById(sessionId)
                .map(this::mapToVaultiqSession).orElse(null);

        if (session == null) {
            log.info("Session '{}' not found in database.", sessionId);
        } else {
            log.debug("Session '{}' loaded from database.", sessionId);
        }

        return session;
    }

    /**
     * Retrieves the device fingerprint for a specific session from the database.
     *
     * @param sessionId The unique identifier of the session to retrieve the fingerprint for.
     * @return Optional String representing the device fingerprint. Returns {@code null} if no session exists with the given ID in the database.
     */
    public Optional<String> getSessionFingerprint(String sessionId) {
        return Optional.ofNullable(get(sessionId)).map(ClientSession::getDeviceFingerPrint);
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
     * @return A {@link List} of {@link ClientSession} DTOs for the user. Returns an empty list if no sessions are found.
     */
    public List<ClientSession> list(String userId) {
        log.debug("Fetching sessions for user '{}'.", userId);

        // Find all entities by user ID and map them to ClientSession DTOs.
        return sessionRepository.findAllByUserId(userId).stream()
                .map(this::mapToVaultiqSession)
                .toList();
    }

    /**
     * Retrieves all active Vaultiq sessions for a specific user from the database.
     *
     * @param userId The unique identifier of the user whose sessions are to be retrieved.
     * @return A {@link List} of {@link ClientSession} DTOs for the user. Returns an empty list if no sessions are found.
     */
    public List<ClientSession> getActiveSessionsByUser(String userId) {
        log.debug("Fetching active sessions for user '{}'.", userId);

        return sessionRepository.findAllByUserIdAndIsRevoked(userId, false).stream()
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
     * Deletes all revoked Vaultiq sessions from the database that have been revoked before a specified timestamp.
     * Uses batch processing to avoid memory issues with large datasets.
     *
     * @param retentionPeriod The duration (UTC) before which sessions are to be deleted.
     * @return the number of records deleted
     */
    @Transactional
    public int deleteAllRevokedSessions(Duration retentionPeriod) {
        if (retentionPeriod == null || retentionPeriod.isNegative() || retentionPeriod.isZero()) {
            log.warn("Invalid retention period for cleanup: {}", retentionPeriod);
            return 0;
        }
        
        Instant cutoffTime = BatchProcessingHelper.calculateCutoffTime(retentionPeriod);
        log.debug("Starting batch deletion of client sessions older than {}", cutoffTime);
        
        return BatchProcessingHelper.batchDelete(
                pageable -> sessionRepository.findByRevokedAtBeforeAndIsRevokedTrue(cutoffTime, pageable),
                sessionRepository,
                "client sessions"
        );
    }

    /**
     * Maps a JPA {@link ClientSessionEntity} entity to a client-facing {@link ClientSession} DTO.
     *
     * @param entity The JPA entity to map.
     * @return The corresponding {@link ClientSession} DTO.
     */
    private ClientSession mapToVaultiqSession(ClientSessionEntity entity) {
        return ClientSession.builder()
                .sessionId(entity.getSessionId())
                .userId(entity.getUserId())
                .deviceFingerPrint(entity.getDeviceFingerPrint())
                .createdAt(entity.getCreatedAt())
                .isBlocked(entity.isRevoked())
                .blockedAt(entity.getRevokedAt())
                .build();
    }
}
