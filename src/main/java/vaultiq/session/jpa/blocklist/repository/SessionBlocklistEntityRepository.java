
package vaultiq.session.jpa.blocklist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vaultiq.session.jpa.blocklist.model.SessionBlocklistEntity;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for SessionBlocklistEntity.
 * <p>
 * Provides methods to access session blocklist information via derived queries.
 */
public interface SessionBlocklistEntityRepository extends JpaRepository<SessionBlocklistEntity, String> {

    /**
     * Finds all blocklisted session entities for the specified user.
     *
     * @param userId the user identifier
     * @return list of blocklisted sessions for the user
     */
    List<SessionBlocklistEntity> findAllByUserId(String userId);

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
    boolean existsByUserIdAndBlocklistedAtGreaterThan(String userId, Instant timestamp);
}
