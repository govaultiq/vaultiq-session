package vaultiq.session.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vaultiq.session.jpa.model.JpaVaultiqSession;

import java.util.List;

public interface VaultiqSessionRepository extends JpaRepository<JpaVaultiqSession, String> {

    /**
     * Useful when deleting the sessions related to user is required
     */
    List<JpaVaultiqSession> findAllByUserId(String userId);

}
