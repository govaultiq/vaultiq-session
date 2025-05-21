package vaultiq.session.jpa.session.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vaultiq.session.jpa.config.VaultiqSessionAutoConfigurationJpa;
import vaultiq.session.jpa.session.model.VaultiqSessionEntity;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Data JPA repository for managing {@link VaultiqSessionEntity} entities.
 * <p>
 * Provides standard data access operations for Vaultiq session data when JPA
 * persistence is enabled.
 * </p>
 *
 * @see JpaRepository
 * @see VaultiqSessionEntity
 * @see VaultiqSessionAutoConfigurationJpa
 */
public interface VaultiqSessionEntityRepository extends JpaRepository<VaultiqSessionEntity, String> {

    /**
     * Finds all session entities belonging to a specific user.
     *
     * @param userId The unique identifier of the user.
     * @return A list of session entities for the user.
     */
    List<VaultiqSessionEntity> findAllByUserId(String userId);

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
    List<VaultiqSessionEntity> findAllByUserIdAndIsRevoked(String userId, boolean isRevoked);
}
