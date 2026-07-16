package br.com.stowup.support;

import br.com.stowup.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_conversations")
public class SupportConversation {
  @Id
  @Column(length = 36)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private UserAccount customer;

  @Column(nullable = false, unique = true, length = 24)
  private String ticketNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private SupportCategory category;

  @Column(nullable = false, length = 140)
  private String subject;

  @Column(nullable = false, length = 20)
  private String status = "OPEN";

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  protected SupportConversation() {}

  public SupportConversation(UserAccount customer) {
    this(customer, SupportCategory.OTHER, "Atendimento");
  }

  public SupportConversation(UserAccount customer, SupportCategory category, String subject) {
    this.id = UUID.randomUUID().toString();
    this.ticketNumber = "SUP-" + id.replace("-", "").substring(0, 8).toUpperCase();
    this.customer = customer;
    this.category = category;
    this.subject = subject.trim();
  }

  public String getId() {
    return id;
  }

  public UserAccount getCustomer() {
    return customer;
  }

  public String getTicketNumber() {
    return ticketNumber;
  }

  public SupportCategory getCategory() {
    return category;
  }

  public String getSubject() {
    return subject;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void touch() {
    updatedAt = Instant.now();
  }

  public void close() {
    status = "CLOSED";
    touch();
  }

  public void reopen() {
    status = "OPEN";
    touch();
  }
}
