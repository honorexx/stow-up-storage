package br.com.stowup.notification;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationRepository notifications;

  public NotificationController(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @GetMapping
  @Transactional(readOnly = true)
  public NotificationPage mine(Principal principal) {
    List<NotificationView> items =
        notifications.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(principal.getName()).stream()
            .limit(100)
            .map(NotificationView::from)
            .toList();
    return new NotificationPage(
        notifications.countByUserEmailIgnoreCaseAndReadAtIsNull(principal.getName()), items);
  }

  @PostMapping("/{id}/read")
  @Transactional
  public NotificationView read(@PathVariable Long id, Principal principal) {
    Notification item =
        notifications
            .findByIdAndUserEmailIgnoreCase(id, principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    item.read();
    return NotificationView.from(item);
  }

  @PostMapping("/read-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void readAll(Principal principal) {
    notifications
        .findByUserEmailIgnoreCaseOrderByCreatedAtDesc(principal.getName())
        .forEach(Notification::read);
  }

  public record NotificationPage(long unread, List<NotificationView> items) {}

  public record NotificationView(
      Long id,
      String type,
      String title,
      String message,
      String referenceType,
      String referenceId,
      boolean read,
      Instant createdAt) {
    static NotificationView from(Notification n) {
      return new NotificationView(
          n.getId(),
          n.getType(),
          n.getTitle(),
          n.getMessage(),
          n.getReferenceType(),
          n.getReferenceId(),
          n.getReadAt() != null,
          n.getCreatedAt());
    }
  }
}
