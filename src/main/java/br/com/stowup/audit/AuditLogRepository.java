package br.com.stowup.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  List<AuditLog> findTop200ByOrderByCreatedAtDesc();

  List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
      String entityType, String entityId);
}
