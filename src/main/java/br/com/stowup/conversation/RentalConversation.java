package br.com.stowup.conversation;

import br.com.stowup.space.Space;
import br.com.stowup.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "rental_conversations",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_rental_conversation",
            columnNames = {"space_id", "customer_id"}))
public class RentalConversation {
  @Id
  @Column(length = 36)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "space_id")
  private Space space;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id")
  private UserAccount customer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id")
  private UserAccount owner;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  protected RentalConversation() {}

  public RentalConversation(Space space, UserAccount customer, UserAccount owner) {
    this.id = UUID.randomUUID().toString();
    this.space = space;
    this.customer = customer;
    this.owner = owner;
  }

  public String getId() {
    return id;
  }

  public Space getSpace() {
    return space;
  }

  public UserAccount getCustomer() {
    return customer;
  }

  public UserAccount getOwner() {
    return owner;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void touch() {
    updatedAt = Instant.now();
  }

  public boolean includes(String email) {
    return customer.getEmail().equalsIgnoreCase(email) || owner.getEmail().equalsIgnoreCase(email);
  }

  public UserAccount other(String email) {
    return customer.getEmail().equalsIgnoreCase(email) ? owner : customer;
  }
}
