package br.com.stowup.notification;

import br.com.stowup.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(nullable = false, length = 40)
  private String type;

  @Column(nullable = false, length = 140)
  private String title;

  @Column(nullable = false, length = 600)
  private String message;

  @Column(name = "reference_type", length = 40)
  private String referenceType;

  @Column(name = "reference_id", length = 60)
  private String referenceId;

  @Column(name = "read_at")
  private Instant readAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected Notification() {}

  public Notification(
      UserAccount user,
      String type,
      String title,
      String message,
      String referenceType,
      String referenceId) {
    this.user = user;
    this.type = type;
    this.title = title;
    this.message = message;
    this.referenceType = referenceType;
    this.referenceId = referenceId;
  }

  public void read() {
    if (readAt == null) readAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getMessage() {
    return message;
  }

  public String getReferenceType() {
    return referenceType;
  }

  public String getReferenceId() {
    return referenceId;
  }

  public Instant getReadAt() {
    return readAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
