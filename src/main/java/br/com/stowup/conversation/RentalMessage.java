package br.com.stowup.conversation;

import br.com.stowup.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rental_messages")
public class RentalMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conversation_id")
  private RentalConversation conversation;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender_id")
  private UserAccount sender;

  @Column(nullable = false, length = 2000)
  private String body;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected RentalMessage() {}

  public RentalMessage(RentalConversation conversation, UserAccount sender, String body) {
    this.conversation = conversation;
    this.sender = sender;
    this.body = body.trim();
  }

  public Long getId() {
    return id;
  }

  public UserAccount getSender() {
    return sender;
  }

  public String getBody() {
    return body;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
