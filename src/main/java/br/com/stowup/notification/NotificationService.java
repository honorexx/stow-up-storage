package br.com.stowup.notification;

import br.com.stowup.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final NotificationRepository notifications;

  public NotificationService(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @Transactional
  public void send(
      UserAccount user,
      String type,
      String title,
      String message,
      String referenceType,
      Object referenceId) {
    notifications.save(
        new Notification(
            user,
            type,
            title,
            message,
            referenceType,
            referenceId == null ? null : String.valueOf(referenceId)));
  }
}
