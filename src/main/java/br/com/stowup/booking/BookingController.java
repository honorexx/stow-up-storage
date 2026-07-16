package br.com.stowup.booking;

import br.com.stowup.audit.AuditService;
import br.com.stowup.contract.RentalContractRepository;
import br.com.stowup.notification.NotificationService;
import br.com.stowup.space.Space;
import br.com.stowup.space.SpaceRepository;
import br.com.stowup.user.UserAccount;
import br.com.stowup.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
  private final BookingRepository bookings;
  private final SpaceRepository spaces;
  private final UserRepository users;
  private final NotificationService notifications;
  private final AuditService audit;
  private final RentalContractRepository contracts;

  public BookingController(
      BookingRepository bookings,
      SpaceRepository spaces,
      UserRepository users,
      NotificationService notifications,
      AuditService audit,
      RentalContractRepository contracts) {
    this.bookings = bookings;
    this.spaces = spaces;
    this.users = users;
    this.notifications = notifications;
    this.audit = audit;
    this.contracts = contracts;
  }

  @GetMapping
  @Transactional(readOnly = true)
  public List<BookingView> mine(
      @RequestParam(defaultValue = "ALL") String category, Principal principal) {
    return deduplicateActive(
            bookings.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(principal.getName()))
        .stream()
        .map(BookingView::from)
        .filter(
            view -> "ALL".equalsIgnoreCase(category) || view.category().equalsIgnoreCase(category))
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public BookingView create(@Valid @RequestBody CreateBookingRequest request, Principal principal) {
    UserAccount customer =
        users
            .findByEmailIgnoreCase(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    Space space =
        spaces
            .findByIdForBooking(request.spaceId())
            .filter(item -> item.isAvailable() && "ACTIVE".equals(item.getLifecycleStatus()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT, "O espaço não está disponível."));
    if (space.getOwner().getEmail().equalsIgnoreCase(principal.getName()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Você não pode reservar o próprio espaço.");
    Booking reusable =
        bookings
            .findBySpaceIdAndCustomerIdAndStartDateAndMonthsAndStatusInOrderByCreatedAtDesc(
                space.getId(),
                customer.getId(),
                request.start(),
                request.months(),
                EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
            .stream()
            .reduce(this::preferred)
            .orElse(null);
    if (reusable != null) return BookingView.from(reusable);
    LocalDate end = request.start().plusMonths(request.months());

    BigDecimal subtotal = space.getPrice().multiply(BigDecimal.valueOf(request.months()));
    BigDecimal fee = subtotal.multiply(new BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP);
    String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    Booking booking =
        new Booking(
            "RSV-" + suffix,
            space,
            customer,
            request.start(),
            request.months(),
            subtotal.add(fee),
            BookingStatus.PENDING,
            "CTR-" + suffix,
            request.paymentMethod());
    Booking saved = bookings.save(booking);
    notifications.send(
        space.getOwner(),
        "BOOKING_CREATED",
        "Nova solicitação de reserva",
        customer.getName() + " solicitou o espaço " + space.getTitle() + ".",
        "BOOKING",
        saved.getId());
    audit.record(
        principal.getName(),
        "BOOKING_CREATED",
        "BOOKING",
        saved.getId(),
        request.start() + " a " + end);
    return BookingView.from(saved);
  }

  private List<Booking> deduplicateActive(List<Booking> source) {
    Map<String, Booking> visible = new LinkedHashMap<>();
    for (Booking booking : source) {
      boolean active =
          booking.getStatus() == BookingStatus.PENDING
              || booking.getStatus() == BookingStatus.CONFIRMED;
      String key =
          active
              ? booking.getSpace().getId()
                  + "|"
                  + booking.getStartDate()
                  + "|"
                  + booking.getMonths()
              : booking.getId();
      visible.merge(key, booking, this::preferred);
    }
    return List.copyOf(visible.values());
  }

  private Booking preferred(Booking first, Booking second) {
    return bookingPriority(second) > bookingPriority(first) ? second : first;
  }

  private int bookingPriority(Booking booking) {
    int bookingScore = booking.getStatus() == BookingStatus.CONFIRMED ? 100 : 0;
    int contractScore =
        contracts
            .findByBookingId(booking.getId())
            .map(
                contract ->
                    switch (contract.getStatus()) {
                      case "APPROVED" -> 40;
                      case "PENDING_REVIEW" -> 30;
                      case "GENERATED" -> 20;
                      case "REJECTED" -> 10;
                      default -> 0;
                    })
            .orElse(0);
    return bookingScore + contractScore;
  }

  @GetMapping("/availability/{spaceId}")
  @Transactional(readOnly = true)
  public List<AvailabilityView> availability(@PathVariable Long spaceId) {
    return bookings.availability(spaceId).stream()
        .map(b -> new AvailabilityView(b.getStartDate(), b.getEndDate()))
        .toList();
  }

  @PostMapping("/{id}/checkout")
  @Transactional
  public BookingView checkout(@PathVariable String id, Principal principal) {
    Booking b =
        bookings
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Locação não encontrada."));
    boolean customer = b.getCustomer().getEmail().equalsIgnoreCase(principal.getName());
    boolean owner = b.getSpace().getOwner().getEmail().equalsIgnoreCase(principal.getName());
    if (!customer && !owner) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    if (b.getStatus() != BookingStatus.CONFIRMED)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente uma locação ativa pode ser encerrada.");
    boolean completed = b.confirmCheckout(customer, LocalDate.now());
    if (completed) {
      b.getSpace().release(b.getReservedCapacity());
      notifications.send(
          b.getCustomer(),
          "REVIEW_AVAILABLE",
          "Avaliação disponível",
          "A locação foi encerrada. Você tem 14 dias para avaliar a experiência.",
          "BOOKING",
          b.getId());
      notifications.send(
          b.getSpace().getOwner(),
          "REVIEW_AVAILABLE",
          "Avaliação disponível",
          "A locação foi encerrada. Você tem 14 dias para avaliar o cliente.",
          "BOOKING",
          b.getId());
    } else if (customer) {
      notifications.send(
          b.getSpace().getOwner(),
          "OWNER_CHECKOUT_REQUIRED",
          "Confirme a retirada",
          b.getCustomer().getName() + " informou a retirada antecipada. Confirme o encerramento.",
          "BOOKING",
          b.getId());
    } else {
      notifications.send(
          b.getCustomer(),
          "CUSTOMER_CHECKOUT_REQUIRED",
          "Confirme o encerramento",
          "O proprietário confirmou a retirada. Confirme o check-out no aplicativo.",
          "BOOKING",
          b.getId());
    }
    audit.record(
        principal.getName(),
        "BOOKING_CHECKOUT_CONFIRMED",
        "BOOKING",
        id,
        completed ? "Locação concluída" : "Aguardando a outra parte");
    return BookingView.from(b);
  }

  @PostMapping("/{id}/cancel")
  @Transactional
  public BookingView cancel(@PathVariable String id, Principal principal) {
    Booking booking =
        bookings
            .findByIdForUpdate(id)
            .filter(item -> item.getCustomer().getEmail().equalsIgnoreCase(principal.getName()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada."));
    if (booking.getStatus() != BookingStatus.PENDING)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente uma reserva pendente pode ser cancelada.");
    booking.cancel();
    notifications.send(
        booking.getSpace().getOwner(),
        "BOOKING_CANCELLED",
        "Solicitação cancelada",
        booking.getCustomer().getName()
            + " cancelou a solicitação para "
            + booking.getSpace().getTitle()
            + ".",
        "BOOKING",
        booking.getId());
    audit.record(
        principal.getName(),
        "BOOKING_CANCELLED",
        "BOOKING",
        booking.getId(),
        "Cancelada pelo cliente");
    return BookingView.from(booking);
  }

  public record CreateBookingRequest(
      @NotNull Long spaceId,
      @NotNull @FutureOrPresent LocalDate start,
      @Min(1) @Max(24) int months,
      @NotBlank String paymentMethod) {}

  public record AvailabilityView(LocalDate start, LocalDate end) {}

  public record BookingView(
      String id,
      Long spaceId,
      String spaceTitle,
      LocalDate start,
      LocalDate end,
      int months,
      BigDecimal total,
      String status,
      String category,
      boolean reviewAvailable,
      String contract,
      String paymentMethod) {
    static BookingView from(Booking booking) {
      String statusPt =
          switch (booking.getStatus()) {
            case CONFIRMED -> "Confirmada";
            case PENDING -> "Pendente";
            case CANCELLED -> "Cancelada";
            case COMPLETED -> "Concluída";
          };
      String category =
          booking.getStatus() == BookingStatus.COMPLETED
              ? "COMPLETED"
              : booking.getStatus() == BookingStatus.CANCELLED
                  ? "CANCELLED"
                  : booking.getStatus() == BookingStatus.CONFIRMED ? "ACTIVE" : "PENDING";
      boolean reviewAvailable =
          booking.getStatus() == BookingStatus.COMPLETED
              && booking.getCompletedAt() != null
              && java.time.Instant.now()
                  .isBefore(booking.getCompletedAt().plus(14, java.time.temporal.ChronoUnit.DAYS));
      return new BookingView(
          booking.getId(),
          booking.getSpace().getId(),
          booking.getSpace().getTitle(),
          booking.getStartDate(),
          booking.getEndDate(),
          booking.getMonths(),
          booking.getTotal(),
          statusPt,
          category,
          reviewAvailable,
          booking.getContractCode(),
          booking.getPaymentMethod());
    }
  }
}
