package vaultiq.session.jpa.session.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vaultiq.session.jpa.config.SessionAutoConfigurationJpa;
import vaultiq.session.jpa.session.model.ClientSessionEntity;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Spring Data JPA repository for managing {@link ClientSessionEntity} entities.
 * <p>
 * Provides standard data access operations for Vaultiq session data when JPA
 * persistence is enabled.
 * </p>
 *
 * @see JpaRepository
 * @see ClientSessionEntity
 * @see SessionAutoConfigurationJpa
 */
public interface ClientSessionEntityRepository extends JpaRepository<ClientSessionEntity, String> {

    /**
     * Finds all session entities belonging to a specific user.
     *
     * @param userId The unique identifier of the user.
     * @return A list of session entities for the user.
     */
    List<ClientSessionEntity> findAllByUserId(String userId);

    /**
     * Counts the number of session entities belonging to a specific user.
     *
     * @param userId The unique identifier of the user.
     * @return The total count of sessions for the user.
     */
    int countByUserId(String userId);

    /**
     * Finds all session entities belonging to a specific user and are revoked.
     *
     * @param userId    The unique identifier of the user.
     * @param isRevoked The revoked status of the sessions.
     * @return A list of session entities for the user that are revoked.
     */
    List<ClientSessionEntity> findAllByUserIdAndIsRevoked(String userId, boolean isRevoked);

    List<ClientSessionEntity> findAllByUserIdAndIsRevokedFalseAndSessionIdNotIn(
            String userId,
            Set<String> excludedIds
    );

    /**
     * Deletes all session entities that have been revoked before a specified timestamp.
     *
     * @param cutoffTime The timestamp (UTC) before which sessions are to be deleted.
     */
    void deleteByRevokedAtBefore(Instant cutoffTime);
    
    /**
     * Finds revoked client sessions created before a specified time, with pagination.
     *
     * @param cutoffTime the cutoff time
     * @param pageable the pagination information
     * @return a list of client session entities
     */
    List<ClientSessionEntity> findByRevokedAtBeforeAndIsRevokedTrue(Instant cutoffTime, Pageable pageable);
}
