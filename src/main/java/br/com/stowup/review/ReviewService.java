package br.com.stowup.review;

import br.com.stowup.audit.AuditService;
import br.com.stowup.booking.*;
import br.com.stowup.notification.NotificationService;
import br.com.stowup.user.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewService {
  private final RentalReviewRepository reviews;
  private final BookingRepository bookings;
  private final UserRepository users;
  private final NotificationService notifications;
  private final AuditService audit;

  public ReviewService(
      RentalReviewRepository reviews,
      BookingRepository bookings,
      UserRepository users,
      NotificationService notifications,
      AuditService audit) {
    this.reviews = reviews;
    this.bookings = bookings;
    this.users = users;
    this.notifications = notifications;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public List<EligibleReview> eligible(String email) {
    List<Booking> all = new ArrayList<>();
    all.addAll(bookings.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email));
    all.addAll(bookings.findBySpaceOwnerEmailIgnoreCaseOrderByCreatedAtDesc(email));
    return all.stream()
        .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getCompletedAt() != null)
        .distinct()
        .map(b -> eligibleView(b, email))
        .filter(EligibleReview::eligible)
        .toList();
  }

  @Transactional
  public ReviewView create(String bookingId, String email, int rating, String comment) {
    Booking b =
        bookings
            .findById(bookingId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Locação não encontrada."));
    EligibleReview e = eligibleView(b, email);
    if (!e.eligible()) throw new ResponseStatusException(HttpStatus.CONFLICT, e.reason());
    UserAccount reviewer = users.findByEmailIgnoreCase(email).orElseThrow();
    boolean customer = b.getCustomer().getEmail().equalsIgnoreCase(email);
    UserAccount reviewed = customer ? b.getSpace().getOwner() : b.getCustomer();
    RentalReview saved =
        reviews.save(
            new RentalReview(
                b,
                reviewer,
                reviewed,
                customer ? "CUSTOMER_TO_OWNER" : "OWNER_TO_CUSTOMER",
                rating,
                comment == null ? null : comment.trim()));
    if (customer) {
      Double avg = reviews.averageForSpace(b.getSpace().getId());
      long count = reviews.countBySpaceIdAndDirection(b.getSpace().getId(), "CUSTOMER_TO_OWNER");
      b.getSpace().setRatingSummary(avg == null ? 0 : avg, (int) count);
    }
    notifications.send(
        reviewed,
        "REVIEW_RECEIVED",
        "Nova avaliação recebida",
        reviewer.getName() + " avaliou a experiência da locação.",
        "BOOKING",
        b.getId());
    audit.record(email, "REVIEW_CREATED", "BOOKING", b.getId(), "Nota " + rating);
    return ReviewView.from(saved);
  }

  private EligibleReview eligibleView(Booking b, String email) {
    boolean participant =
        b.getCustomer().getEmail().equalsIgnoreCase(email)
            || b.getSpace().getOwner().getEmail().equalsIgnoreCase(email);
    if (!participant)
      return new EligibleReview(
          b.getId(), b.getSpace().getTitle(), false, "Você não participa desta locação.", null);
    if (b.getStatus() != BookingStatus.COMPLETED || b.getCompletedAt() == null)
      return new EligibleReview(
          b.getId(),
          b.getSpace().getTitle(),
          false,
          "A avaliação será liberada após o encerramento da locação.",
          null);
    Instant deadline = b.getCompletedAt().plus(14, ChronoUnit.DAYS);
    if (Instant.now().isAfter(deadline))
      return new EligibleReview(
          b.getId(),
          b.getSpace().getTitle(),
          false,
          "O prazo de 14 dias para avaliar terminou.",
          deadline);
    if (reviews.existsByBookingIdAndReviewerEmailIgnoreCase(b.getId(), email))
      return new EligibleReview(
          b.getId(), b.getSpace().getTitle(), false, "Avaliação já enviada.", deadline);
    return new EligibleReview(
        b.getId(), b.getSpace().getTitle(), true, "Avaliação disponível.", deadline);
  }

  public record EligibleReview(
      String bookingId, String spaceTitle, boolean eligible, String reason, Instant deadline) {}

  public record ReviewView(
      Long id,
      String bookingId,
      String reviewer,
      String reviewedUser,
      String direction,
      int rating,
      String comment,
      Instant createdAt) {
    static ReviewView from(RentalReview r) {
      return new ReviewView(
          r.getId(),
          r.getBooking().getId(),
          r.getReviewer().getName(),
          r.getReviewedUser().getName(),
          r.getDirection(),
          r.getRating(),
          r.getComment(),
          r.getCreatedAt());
    }
  }
}
