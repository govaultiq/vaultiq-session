
package vaultiq.session.jpa.revoke.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vaultiq.session.jpa.revoke.model.RevokedSessionEntity;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for RevokedSessionEntity.
 * <p>
 * Provides methods to access session revoke information via derived queries.
 */
public interface RevokedSessionEntityRepository extends JpaRepository<RevokedSessionEntity, String> {

    /**
     * Finds all blocklisted session entities for the specified user.
     *
     * @param userId the user identifier
     * @return list of blocklisted sessions for the user
     */
    List<RevokedSessionEntity> findAllByUserId(String userId);

    /**
     * Counts the number of blocklisted session entities for the specified user.
     *
     * @param userId the user identifier
     * @return the total number of blocklisted sessions for the user
     */
    long countByUserId(String userId);

    /**
     * Checks if any blocklisted session for the user exists that was created after the specified timestamp.
     *
     * @param userId    the user identifier
     * @param timestamp the lower bound for the blocklistedAt value (exclusive)
     * @return true if at least one such session exists, false otherwise
     */
    boolean existsByUserIdAndRevokedAtGreaterThan(String userId, Instant timestamp);

    /**
     * Deletes all blocklisted session entities older than the specified retention period.
     *
     * @param cutoffTime the cutoff timestamp (exclusive)
     */
    void deleteByRevokedAtBefore(Instant cutoffTime);
    
    /**
     * Finds revoked sessions created before a specified time, with pagination.
     *
     * @param cutoffTime the cutoff time
     * @param pageable the pagination information
     * @return a list of revoked session entities
     */
    List<RevokedSessionEntity> findByRevokedAtBefore(Instant cutoffTime, Pageable pageable);
}
