package vaultiq.session.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vaultiq.session.jpa.model.VaultiqSession;

import java.util.List;

public interface VaultiqSessionRepository extends JpaRepository<VaultiqSession, String> {

    /**
     * Useful when deleting the sessions related to user is required
     */
    List<VaultiqSession> findAllByUserId(String userId);

}
