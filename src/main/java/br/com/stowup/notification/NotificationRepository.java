package br.com.stowup.notification;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);

  Optional<Notification> findByIdAndUserEmailIgnoreCase(Long id, String email);

  long countByUserEmailIgnoreCaseAndReadAtIsNull(String email);
}
