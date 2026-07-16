package br.com.stowup.owner;

import br.com.stowup.account.application.AccountService;
import br.com.stowup.audit.AuditService;
import br.com.stowup.booking.Booking;
import br.com.stowup.booking.BookingRepository;
import br.com.stowup.booking.BookingStatus;
import br.com.stowup.listingdocument.application.ListingDocumentService;
import br.com.stowup.media.ImageStorage;
import br.com.stowup.space.Space;
import br.com.stowup.space.SpaceRepository;
import br.com.stowup.user.UserAccount;
import br.com.stowup.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/owner")
public class OwnerController {
  private final SpaceRepository spaces;
  private final UserRepository users;
  private final BookingRepository bookings;
  private final AccountService accounts;
  private final ListingDocumentService listingDocuments;
  private final ImageStorage images;
  private final int minimumPhotos;
  private final AuditService audit;

  public OwnerController(
      SpaceRepository spaces,
      UserRepository users,
      BookingRepository bookings,
      AccountService accounts,
      ListingDocumentService listingDocuments,
      AuditService audit,
      ImageStorage images,
      @Value("${stowup.listings.minimum-photos:4}") int minimumPhotos) {
    this.spaces = spaces;
    this.users = users;
    this.bookings = bookings;
    this.accounts = accounts;
    this.listingDocuments = listingDocuments;
    this.audit = audit;
    this.images = images;
    this.minimumPhotos = minimumPhotos;
  }

  @GetMapping("/spaces")
  @Transactional(readOnly = true)
  public List<OwnerSpaceView> spaces(Principal principal) {
    return this.spaces.findByOwnerEmailIgnoreCaseOrderByIdDesc(principal.getName()).stream()
        .filter(space -> !"WITHDRAWN".equals(space.getLifecycleStatus()))
        .map(this::view)
        .toList();
  }

  @GetMapping("/dashboard")
  @Transactional(readOnly = true)
  public OwnerDashboard dashboard(Principal principal) {
    List<Space> ownedSpaces =
        spaces.findByOwnerEmailIgnoreCaseOrderByIdDesc(principal.getName()).stream()
            .filter(space -> !"WITHDRAWN".equals(space.getLifecycleStatus()))
            .toList();
    List<Booking> ownerBookings =
        bookings.findBySpaceOwnerEmailIgnoreCaseOrderByCreatedAtDesc(principal.getName());
    long activeSpaces =
        ownedSpaces.stream().filter(s -> "ACTIVE".equals(s.getLifecycleStatus())).count();
    long occupiedSpaces =
        ownedSpaces.stream().filter(s -> "OCCUPIED".equals(s.getLifecycleStatus())).count();
    long pendingReservations =
        ownerBookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
    long activeContracts =
        ownerBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
    BigDecimal revenue =
        ownerBookings.stream()
            .filter(
                booking ->
                    booking.getStatus() == BookingStatus.CONFIRMED
                        || booking.getStatus() == BookingStatus.COMPLETED)
            .map(Booking::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    YearMonth current = YearMonth.now();
    BigDecimal monthRevenue =
        ownerBookings.stream()
            .filter(
                b ->
                    b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.COMPLETED)
            .filter(
                b ->
                    YearMonth.from(b.getCreatedAt().atZone(ZoneId.systemDefault())).equals(current))
            .map(Booking::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    int occupancyRate =
        ownedSpaces.isEmpty()
            ? 0
            : (int)
                Math.round(
                    ownedSpaces.stream().mapToInt(Space::getCapacityUsed).sum()
                        * 100.0
                        / Math.max(
                            1, ownedSpaces.stream().mapToInt(Space::getCapacityTotal).sum()));
    List<UpcomingContract> upcoming =
        ownerBookings.stream()
            .filter(
                b ->
                    b.getStatus() == BookingStatus.CONFIRMED
                        && b.getStartDate().isAfter(LocalDate.now()))
            .sorted(java.util.Comparator.comparing(Booking::getStartDate))
            .limit(5)
            .map(UpcomingContract::from)
            .toList();
    List<ActiveRental> active =
        ownerBookings.stream()
            .filter(
                b ->
                    b.getStatus() == BookingStatus.CONFIRMED
                        && !b.getStartDate().isAfter(LocalDate.now()))
            .map(ActiveRental::from)
            .toList();
    return new OwnerDashboard(
        activeSpaces,
        occupiedSpaces,
        pendingReservations,
        activeContracts,
        monthRevenue,
        revenue,
        occupancyRate,
        upcoming,
        active,
        ownedSpaces.stream().map(this::view).toList());
  }

  @GetMapping("/financial-report")
  @Transactional(readOnly = true)
  public FinancialReport financialReport(Principal principal) {
    List<Booking> paid =
        bookings.findBySpaceOwnerEmailIgnoreCaseOrderByCreatedAtDesc(principal.getName()).stream()
            .filter(
                b ->
                    b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.COMPLETED)
            .toList();
    BigDecimal total =
        paid.stream().map(Booking::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    long completed = paid.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
    BigDecimal average =
        paid.isEmpty()
            ? BigDecimal.ZERO
            : total.divide(BigDecimal.valueOf(paid.size()), 2, java.math.RoundingMode.HALF_UP);
    Map<String, BigDecimal> monthly =
        paid.stream()
            .collect(
                Collectors.groupingBy(
                    b -> YearMonth.from(b.getCreatedAt().atZone(ZoneId.systemDefault())).toString(),
                    java.util.TreeMap::new,
                    Collectors.reducing(BigDecimal.ZERO, Booking::getTotal, BigDecimal::add)));
    return new FinancialReport(total, completed, average, monthly);
  }

  @GetMapping("/rental-history")
  @Transactional(readOnly = true)
  public List<RentalHistory> history(Principal principal) {
    return bookings
        .findBySpaceOwnerEmailIgnoreCaseOrderByCreatedAtDesc(principal.getName())
        .stream()
        .map(RentalHistory::from)
        .toList();
  }

  @PostMapping("/spaces")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public OwnerSpaceView create(
      @Valid @RequestBody CreateSpaceRequest request, Principal principal) {
    accounts.promoteToOwner(principal.getName());
    UserAccount owner =
        users
            .findByEmailIgnoreCase(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    Space space =
        new Space(
            request.title(),
            request.city(),
            request.district(),
            digits(request.postalCode()),
            request.addressLine(),
            request.addressNumber(),
            request.addressComplement(),
            request.state().toUpperCase(),
            request.type(),
            request.size(),
            request.price(),
            0,
            0,
            0,
            request.latitude(),
            request.longitude(),
            false,
            request.description(),
            request.features(),
            "#d8e4ef",
            "#68809c",
            owner);
    Space saved = spaces.saveAndFlush(space);
    saved.setDimensions(request.heightMeters(), request.lengthMeters(), request.depthMeters());
    listingDocuments.createFor(saved);
    audit.record(principal.getName(), "SPACE_CREATED", "SPACE", saved.getId(), saved.getTitle());
    return view(saved);
  }

  @PostMapping(value = "/spaces/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Transactional
  public OwnerSpaceView uploadPhoto(
      @PathVariable Long id, @RequestPart("file") MultipartFile file, Principal principal)
      throws IOException {
    Space space =
        spaces
            .findByIdAndOwnerEmailIgnoreCase(id, principal.getName())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anúncio não encontrado."));
    if (space.getPhotoUrls().size() >= 8)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cada anúncio pode ter até 8 fotos.");
    String url = images.saveSpace(id, file);
    space.addPhoto(url);
    audit.record(principal.getName(), "SPACE_PHOTO_ADDED", "SPACE", id, url);
    return view(space);
  }

  @DeleteMapping("/spaces/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void deleteSpace(
      @PathVariable Long id, @Valid @RequestBody DeleteSpaceRequest request, Principal principal) {
    accounts.requirePassword(principal.getName(), request.password());
    Space space =
        spaces
            .findByIdAndOwnerEmailIgnoreCase(id, principal.getName())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anúncio não encontrado."));
    boolean active =
        bookings.availability(id).stream()
            .anyMatch(
                booking ->
                    booking.getStatus() == BookingStatus.PENDING
                        || booking.getStatus() == BookingStatus.CONFIRMED);
    if (active)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Cancele ou encerre as reservas ativas antes de excluir o anúncio.");
    space.withdrawByOwner();
    audit.record(principal.getName(), "SPACE_OWNER_DELETED", "SPACE", id, space.getTitle());
  }

  @PatchMapping("/spaces/{id}/availability")
  @Transactional
  public OwnerSpaceView availability(
      @PathVariable Long id, @RequestBody AvailabilityRequest request, Principal principal) {
    Space space =
        spaces
            .findByIdAndOwnerEmailIgnoreCase(id, principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    try {
      if (request.available()) space.reactivate();
      else space.suspendTemporarily();
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
    audit.record(
        principal.getName(),
        request.available() ? "SPACE_REACTIVATED" : "SPACE_TEMPORARILY_SUSPENDED",
        "SPACE",
        id,
        null);
    return view(space);
  }

  public record CreateSpaceRequest(
      @NotBlank @Size(max = 140) String title,
      @NotBlank String city,
      @NotBlank String district,
      @NotBlank @Pattern(regexp = "^[0-9-]{8,9}$") String postalCode,
      @NotBlank @Size(max = 180) String addressLine,
      @NotBlank @Size(max = 20) String addressNumber,
      @Size(max = 80) String addressComplement,
      @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String state,
      @NotBlank String type,
      @Min(1) @Max(10000) int size,
      @NotNull @DecimalMin("50.00") BigDecimal price,
      @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
      @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
      @NotBlank @Size(max = 1200) String description,
      @NotEmpty List<@NotBlank String> features,
      @NotNull @DecimalMin("0.10") @DecimalMax("100.00") BigDecimal heightMeters,
      @NotNull @DecimalMin("0.10") @DecimalMax("1000.00") BigDecimal lengthMeters,
      @NotNull @DecimalMin("0.10") @DecimalMax("1000.00") BigDecimal depthMeters) {}

  public record OwnerSpaceView(
      Long id,
      String title,
      String city,
      String district,
      String postalCode,
      String addressLine,
      String addressNumber,
      String addressComplement,
      String state,
      String type,
      int size,
      BigDecimal price,
      Double latitude,
      Double longitude,
      boolean verified,
      boolean available,
      List<String> features,
      List<String> photos,
      String description,
      String moderationStatus,
      String moderationReason,
      String documentStatus,
      String lifecycleStatus,
      BigDecimal heightMeters,
      BigDecimal lengthMeters,
      BigDecimal depthMeters,
      BigDecimal volumeCubicMeters,
      int capacityTotal,
      int capacityUsed,
      int capacityAvailable,
      int minimumPhotos,
      boolean photoRequirementMet) {
    static OwnerSpaceView from(Space space, String documentStatus) {
      return new OwnerSpaceView(
          space.getId(),
          space.getTitle(),
          space.getCity(),
          space.getDistrict(),
          space.getPostalCode(),
          space.getAddressLine(),
          space.getAddressNumber(),
          space.getAddressComplement(),
          space.getState(),
          space.getType(),
          space.getSize(),
          space.getPrice(),
          space.getLatitude(),
          space.getLongitude(),
          space.isVerified(),
          space.isAvailable(),
          space.getFeatures(),
          space.getPhotoUrls(),
          space.getDescription(),
          space.getModerationStatus(),
          space.getModerationReason(),
          documentStatus,
          space.getLifecycleStatus(),
          space.getHeightMeters(),
          space.getLengthMeters(),
          space.getDepthMeters(),
          space.getVolumeCubicMeters(),
          space.getCapacityTotal(),
          space.getCapacityUsed(),
          space.getCapacityAvailable(),
          4,
          space.getPhotoUrls().size() >= 4);
    }
  }

  private OwnerSpaceView view(Space space) {
    String status =
        listingDocuments
            .ownerDocument(space.getId(), space.getOwner().getEmail())
            .getStatus()
            .name();
    OwnerSpaceView base = OwnerSpaceView.from(space, status);
    return new OwnerSpaceView(
        base.id(),
        base.title(),
        base.city(),
        base.district(),
        base.postalCode(),
        base.addressLine(),
        base.addressNumber(),
        base.addressComplement(),
        base.state(),
        base.type(),
        base.size(),
        base.price(),
        base.latitude(),
        base.longitude(),
        base.verified(),
        base.available(),
        base.features(),
        base.photos(),
        base.description(),
        base.moderationStatus(),
        base.moderationReason(),
        base.documentStatus(),
        base.lifecycleStatus(),
        base.heightMeters(),
        base.lengthMeters(),
        base.depthMeters(),
        base.volumeCubicMeters(),
        base.capacityTotal(),
        base.capacityUsed(),
        base.capacityAvailable(),
        minimumPhotos,
        space.getPhotoUrls().size() >= minimumPhotos);
  }

  public record AvailabilityRequest(boolean available) {}

  public record DeleteSpaceRequest(@NotBlank String password) {}

  public record OwnerDashboard(
      long activeSpaces,
      long occupiedSpaces,
      long pendingReservations,
      long activeContracts,
      BigDecimal monthRevenue,
      BigDecimal totalRevenue,
      int occupancyRate,
      List<UpcomingContract> upcomingContracts,
      List<ActiveRental> activeRentals,
      List<OwnerSpaceView> spaces) {}

  public record UpcomingContract(
      String bookingId,
      String spaceTitle,
      String customer,
      LocalDate start,
      LocalDate end,
      BigDecimal total) {
    static UpcomingContract from(Booking b) {
      return new UpcomingContract(
          b.getId(),
          b.getSpace().getTitle(),
          b.getCustomer().getName(),
          b.getStartDate(),
          b.getEndDate(),
          b.getTotal());
    }
  }

  public record ActiveRental(
      String bookingId,
      String spaceTitle,
      String customer,
      LocalDate end,
      boolean customerConfirmed,
      boolean ownerConfirmed) {
    static ActiveRental from(Booking b) {
      return new ActiveRental(
          b.getId(),
          b.getSpace().getTitle(),
          b.getCustomer().getName(),
          b.getEndDate(),
          b.getCustomerCheckoutAt() != null,
          b.getOwnerCheckoutAt() != null);
    }
  }

  public record FinancialReport(
      BigDecimal totalRevenue,
      long completedContracts,
      BigDecimal averageRentalValue,
      Map<String, BigDecimal> monthlyRevenue) {}

  public record RentalHistory(
      String bookingId,
      Long spaceId,
      String spaceTitle,
      String customer,
      LocalDate start,
      LocalDate end,
      String status,
      BigDecimal total,
      Instant completedAt) {
    static RentalHistory from(Booking b) {
      return new RentalHistory(
          b.getId(),
          b.getSpace().getId(),
          b.getSpace().getTitle(),
          b.getCustomer().getName(),
          b.getStartDate(),
          b.getEndDate(),
          b.getStatus().name(),
          b.getTotal(),
          b.getCompletedAt());
    }
  }

  private static String digits(String value) {
    return value.replaceAll("\\D", "");
  }
}
