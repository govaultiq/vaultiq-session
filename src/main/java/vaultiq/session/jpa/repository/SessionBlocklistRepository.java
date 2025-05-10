package vaultiq.session.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vaultiq.session.jpa.model.SessionBlocklistEntity;

import java.util.Set;

public interface SessionBlocklistRepository extends JpaRepository<SessionBlocklistEntity, String> {
    Set<SessionBlocklistEntity> findAllByUserId(String userId);
}
