package br.com.stowup.favorite;

import br.com.stowup.space.Space;
import br.com.stowup.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "favorites",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_favorite_user_space",
            columnNames = {"user_id", "space_id"}))
public class Favorite {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "space_id", nullable = false)
  private Space space;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected Favorite() {}

  public Favorite(UserAccount user, Space space) {
    this.user = user;
    this.space = space;
  }

  public Space getSpace() {
    return space;
  }
}
