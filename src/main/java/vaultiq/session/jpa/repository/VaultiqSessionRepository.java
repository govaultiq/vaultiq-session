package vaultiq.session.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vaultiq.session.jpa.model.JpaVaultiqSession;

import java.util.List;
import java.util.Optional;

public interface VaultiqSessionRepository extends JpaRepository<JpaVaultiqSession, String> {

    /**
     * Useful when deleting the sessions related to user is required
     */
    List<JpaVaultiqSession> findAllByUserId(String userId);

    int countByUserId(String userId);

}
