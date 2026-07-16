package br.com.stowup.audit;

import br.com.stowup.user.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
  private final AuditLogRepository logs;
  private final UserRepository users;

  public AuditService(AuditLogRepository logs, UserRepository users) {
    this.logs = logs;
    this.users = users;
  }

  @Transactional
  public void record(String actorEmail, String action, String type, Object id, String details) {
    UserAccount actor =
        actorEmail == null ? null : users.findByEmailIgnoreCase(actorEmail).orElse(null);
    logs.save(new AuditLog(actor, action, type, String.valueOf(id), details));
  }
}
