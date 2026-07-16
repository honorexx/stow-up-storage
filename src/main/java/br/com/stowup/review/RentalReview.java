package br.com.stowup.review;

import br.com.stowup.booking.Booking;
import br.com.stowup.space.Space;
import br.com.stowup.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "rental_reviews",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_review_booking_reviewer",
            columnNames = {"booking_id", "reviewer_id"}))
public class RentalReview {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reviewer_id", nullable = false)
  private UserAccount reviewer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reviewed_user_id", nullable = false)
  private UserAccount reviewedUser;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "space_id", nullable = false)
  private Space space;

  @Column(nullable = false, length = 30)
  private String direction;

  @Column(nullable = false)
  private int rating;

  @Column(length = 1000)
  private String comment;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected RentalReview() {}

  public RentalReview(
      Booking booking,
      UserAccount reviewer,
      UserAccount reviewedUser,
      String direction,
      int rating,
      String comment) {
    this.booking = booking;
    this.reviewer = reviewer;
    this.reviewedUser = reviewedUser;
    this.space = booking.getSpace();
    this.direction = direction;
    this.rating = rating;
    this.comment = comment;
  }

  public Long getId() {
    return id;
  }

  public Booking getBooking() {
    return booking;
  }

  public UserAccount getReviewer() {
    return reviewer;
  }

  public UserAccount getReviewedUser() {
    return reviewedUser;
  }

  public Space getSpace() {
    return space;
  }

  public String getDirection() {
    return direction;
  }

  public int getRating() {
    return rating;
  }

  public String getComment() {
    return comment;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
