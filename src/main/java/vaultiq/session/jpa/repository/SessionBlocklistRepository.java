package vaultiq.session.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vaultiq.session.jpa.model.SessionBlocklistEntity;

import java.util.List;

public interface SessionBlocklistRepository extends JpaRepository<SessionBlocklistEntity, String> {
    List<SessionBlocklistEntity> findAllByUserId(String userId);

    long countByUserId(String userId);
}
