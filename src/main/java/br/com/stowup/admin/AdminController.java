package br.com.stowup.admin;

import br.com.stowup.audit.*;
import br.com.stowup.booking.BookingRepository;
import br.com.stowup.booking.BookingStatus;
import br.com.stowup.contract.RentalContractRepository;
import br.com.stowup.listingdocument.adapter.out.persistence.OwnerListingDocumentRepository;
import br.com.stowup.listingdocument.domain.OwnerListingDocument;
import br.com.stowup.media.ImageStorage;
import br.com.stowup.moderation.application.SpaceModerationService;
import br.com.stowup.space.Space;
import br.com.stowup.space.SpaceRepository;
import br.com.stowup.spacedocument.SpaceDocumentRepository;
import br.com.stowup.support.SupportCategory;
import br.com.stowup.support.SupportConversationRepository;
import br.com.stowup.user.Role;
import br.com.stowup.user.UserAccount;
import br.com.stowup.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final EnumSet<Role> ASSIGNABLE_ROLES =
      EnumSet.of(Role.SUPPORT, Role.LISTING_ANALYST, Role.DOCUMENT_ANALYST, Role.CONTRACT_ANALYST);
  private static final EnumSet<Role> STAFF_ROLES =
      EnumSet.of(
          Role.SUPPORT,
          Role.MODERATOR,
          Role.LISTING_ANALYST,
          Role.DOCUMENT_ANALYST,
          Role.CONTRACT_ANALYST,
          Role.ADMIN);
  private final UserRepository users;
  private final SpaceRepository spaces;
  private final BookingRepository bookings;
  private final PasswordEncoder passwords;
  private final SpaceModerationService moderation;
  private final RentalContractRepository contracts;
  private final SupportConversationRepository support;
  private final AuditLogRepository auditLogs;
  private final AuditService audit;
  private final SpaceDocumentRepository spaceDocuments;
  private final OwnerListingDocumentRepository listingDocuments;
  private final ImageStorage images;

  public AdminController(
      UserRepository users,
      SpaceRepository spaces,
      BookingRepository bookings,
      PasswordEncoder passwords,
      SpaceModerationService moderation,
      RentalContractRepository contracts,
      SupportConversationRepository support,
      AuditLogRepository auditLogs,
      AuditService audit,
      SpaceDocumentRepository spaceDocuments,
      OwnerListingDocumentRepository listingDocuments,
      ImageStorage images) {
    this.users = users;
    this.spaces = spaces;
    this.bookings = bookings;
    this.passwords = passwords;
    this.moderation = moderation;
    this.contracts = contracts;
    this.support = support;
    this.auditLogs = auditLogs;
    this.audit = audit;
    this.spaceDocuments = spaceDocuments;
    this.listingDocuments = listingDocuments;
    this.images = images;
  }

  @GetMapping("/users")
  @Transactional(readOnly = true)
  public List<AppUserView> appUsers() {
    return users.findAllByOrderByCreatedAtDesc().stream()
        .filter(user -> user.getRole() == Role.CUSTOMER || user.getRole() == Role.OWNER)
        .filter(user -> !user.getEmail().endsWith("@deleted.stowup"))
        .map(user -> AppUserView.from(user, spaces.countByOwnerId(user.getId())))
        .toList();
  }

  @PatchMapping("/users/{id}")
  @Transactional
  public AppUserView updateAppUser(
      @PathVariable Long id,
      @Valid @RequestBody UpdateAppUserRequest request,
      Principal principal) {
    UserAccount user = requireAppUser(id);
    if (request.role() != Role.CUSTOMER && request.role() != Role.OWNER)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Perfil de aplicativo inválido.");
    user.setName(request.name().trim());
    user.setPhone(request.phone());
    user.setCity(request.city());
    user.setState(request.state() == null ? null : request.state().trim().toUpperCase());
    user.setRole(request.role());
    user.setEnabled(request.enabled());
    user.setOwnerVerified(request.role() == Role.OWNER && request.ownerVerified());
    audit.record(principal.getName(), "APP_USER_UPDATED", "USER", id, request.role().name());
    return AppUserView.from(user, spaces.countByOwnerId(user.getId()));
  }

  @DeleteMapping("/users/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void deleteAppUser(@PathVariable Long id, Principal principal) {
    UserAccount user = requireAppUser(id);
    boolean activeAsCustomer =
        bookings.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(user.getEmail()).stream()
            .anyMatch(
                b ->
                    b.getStatus() == BookingStatus.PENDING
                        || b.getStatus() == BookingStatus.CONFIRMED);
    boolean activeAsOwner =
        bookings.findBySpaceOwnerEmailIgnoreCaseOrderByCreatedAtDesc(user.getEmail()).stream()
            .anyMatch(
                b ->
                    b.getStatus() == BookingStatus.PENDING
                        || b.getStatus() == BookingStatus.CONFIRMED);
    if (activeAsCustomer || activeAsOwner)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Encerre as reservas e contratos ativos antes de excluir esta conta.");
    Long userId = user.getId();
    spaces.withdrawAllByOwnerId(userId);
    user.close(
        "deleted-user-" + userId + "-" + UUID.randomUUID() + "@deleted.stowup",
        passwords.encode(UUID.randomUUID().toString()));
    users.save(user);
    audit.record(principal.getName(), "APP_USER_DELETED", "USER", userId, "Conta anonimizada");
  }

  @GetMapping("/spaces")
  @Transactional(readOnly = true)
  public List<ManagedSpaceView> allSpaces() {
    return spaces.findAllByOrderByIdDesc().stream().map(this::managedSpaceView).toList();
  }

  @PatchMapping("/spaces/{id}")
  @Transactional
  public ManagedSpaceView updateSpace(
      @PathVariable Long id,
      @Valid @RequestBody UpdateManagedSpaceRequest request,
      Principal principal) {
    Space space =
        spaces
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anúncio não encontrado."));
    try {
      space.updateByAdmin(
          request.title(),
          request.description(),
          request.type(),
          request.heightMeters(),
          request.lengthMeters(),
          request.depthMeters(),
          request.price(),
          request.city(),
          request.district(),
          request.postalCode(),
          request.addressLine(),
          request.addressNumber(),
          request.addressComplement(),
          request.state());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
    audit.record(principal.getName(), "SPACE_ADMIN_UPDATED", "SPACE", id, request.title());
    return managedSpaceView(space);
  }

  @PostMapping(value = "/spaces/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Transactional
  public ManagedSpaceView uploadSpacePhoto(
      @PathVariable Long id, @RequestPart("file") MultipartFile file, Principal principal)
      throws IOException {
    Space space =
        spaces
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anúncio não encontrado."));
    if (space.getPhotoUrls().size() >= 8)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cada anúncio pode ter até 8 fotos.");
    String url = images.saveSpace(id, file);
    space.addPhoto(url);
    audit.record(principal.getName(), "SPACE_ADMIN_PHOTO_ADDED", "SPACE", id, url);
    return managedSpaceView(space);
  }

  @DeleteMapping("/spaces/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void withdrawSpace(@PathVariable Long id, Principal principal) {
    Space space =
        spaces
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anúncio não encontrado."));
    boolean active =
        bookings.availability(id).stream().anyMatch(b -> b.getStatus() == BookingStatus.CONFIRMED);
    if (active)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Um anúncio com locação ativa não pode ser excluído.");
    space.withdrawByAdmin("Removido pelo usuário mestre");
    audit.record(principal.getName(), "SPACE_ADMIN_WITHDRAWN", "SPACE", id, space.getTitle());
  }

  private UserAccount requireAppUser(Long id) {
    UserAccount user =
        users
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    if (user.getRole() != Role.CUSTOMER && user.getRole() != Role.OWNER)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use a área Equipe para contas internas.");
    return user;
  }

  private ManagedSpaceView managedSpaceView(Space space) {
    OwnerListingDocument listingDocument =
        listingDocuments.findBySpaceId(space.getId()).orElse(null);
    return ManagedSpaceView.from(
        space,
        spaceDocuments.countBySpaceId(space.getId()),
        listingDocument == null ? null : listingDocument.getId(),
        listingDocument == null ? null : listingDocument.getStatus().name(),
        listingDocument == null ? null : listingDocument.getOriginalFilename());
  }

  @GetMapping("/dashboard")
  public Map<String, Object> dashboard() {
    List<br.com.stowup.booking.Booking> paid =
        bookings.findAll().stream()
            .filter(
                b ->
                    b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.COMPLETED)
            .toList();
    BigDecimal commission =
        paid.stream()
            .map(
                b ->
                    b.getTotal()
                        .multiply(new BigDecimal("0.08"))
                        .divide(new BigDecimal("1.08"), 2, java.math.RoundingMode.HALF_UP))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("totalUsers", users.count());
    data.put("customers", users.countByRole(Role.CUSTOMER));
    data.put("owners", users.countByRole(Role.OWNER));
    data.put("verifiedOwners", users.countByOwnerVerifiedTrue());
    data.put("activeSpaces", spaces.countByLifecycleStatus("ACTIVE"));
    data.put("occupiedSpaces", spaces.countByLifecycleStatus("OCCUPIED"));
    data.put("pendingSpaces", spaces.countByLifecycleStatus("IN_REVIEW"));
    data.put(
        "ongoingBookings",
        bookings.countByStatus(BookingStatus.PENDING)
            + bookings.countByStatus(BookingStatus.CONFIRMED));
    data.put("activeContracts", contracts.countByStatus("APPROVED"));
    data.put("pendingReports", support.countByCategoryAndStatus(SupportCategory.SECURITY, "OPEN"));
    data.put("platformRevenue", commission);
    return data;
  }

  @GetMapping("/audit")
  @Transactional(readOnly = true)
  public List<AuditView> audit() {
    return auditLogs.findTop200ByOrderByCreatedAtDesc().stream().map(AuditView::from).toList();
  }

  @PostMapping("/owners/{id}/approve")
  @Transactional
  public PendingOwner approve(@PathVariable Long id) {
    UserAccount user =
        users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    user.setRole(Role.OWNER);
    return new PendingOwner(user.getId(), user.getName(), user.getEmail());
  }

  @GetMapping("/spaces/pending")
  @Transactional(readOnly = true)
  public List<ModerationSpaceView> pendingSpaces() {
    return spaces.findByModerationStatusOrderByIdDesc("PENDING").stream()
        .map(ModerationSpaceView::from)
        .toList();
  }

  @PostMapping("/spaces/{id}/approve")
  @Transactional
  public ModerationSpaceView approveSpace(@PathVariable Long id, Principal principal) {
    return ModerationSpaceView.from(moderation.approve(id, principal.getName()));
  }

  @PostMapping("/spaces/{id}/reject")
  @Transactional
  public ModerationSpaceView rejectSpace(
      @PathVariable Long id, @Valid @RequestBody RejectSpaceRequest request, Principal principal) {
    return ModerationSpaceView.from(moderation.reject(id, principal.getName(), request.reason()));
  }

  @PostMapping("/spaces/{id}/suspend")
  @Transactional
  public ModerationSpaceView suspendSpace(
      @PathVariable Long id, @Valid @RequestBody RejectSpaceRequest request, Principal principal) {
    return ModerationSpaceView.from(moderation.suspend(id, principal.getName(), request.reason()));
  }

  @GetMapping("/staff")
  public List<StaffView> staff() {
    return users.findAll().stream()
        .filter(user -> STAFF_ROLES.contains(user.getRole()))
        .map(StaffView::from)
        .toList();
  }

  @PostMapping("/staff")
  @ResponseStatus(HttpStatus.CREATED)
  public StaffView createStaff(@Valid @RequestBody CreateStaffRequest request) {
    if (users.existsByEmailIgnoreCase(request.email()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
    requireAssignableRole(request.role());
    UserAccount staff =
        new UserAccount(
            request.name().trim(),
            request.email().trim(),
            passwords.encode(request.password()),
            request.role());
    staff.setStaffCode(nextStaffCode());
    return StaffView.from(users.save(staff));
  }

  @PatchMapping("/staff/{id}")
  @Transactional
  public StaffView updateStaff(
      @PathVariable Long id, @Valid @RequestBody UpdateStaffRequest request) {
    UserAccount staff = requireManagedStaff(id);
    requireAssignableRole(request.role());
    staff.setName(request.name().trim());
    staff.setRole(request.role());
    staff.setEnabled(request.enabled());
    return StaffView.from(staff);
  }

  @PostMapping("/staff/{id}/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void resetStaffPassword(
      @PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
    requireManagedStaff(id).setPasswordHash(passwords.encode(request.password()));
  }

  @DeleteMapping("/staff/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void deleteStaff(@PathVariable Long id) {
    UserAccount staff = requireManagedStaff(id);
    staff.close(
        "deleted-staff-" + staff.getId() + "-" + UUID.randomUUID() + "@deleted.stowup",
        passwords.encode(UUID.randomUUID().toString()));
  }

  private UserAccount requireManagedStaff(Long id) {
    UserAccount staff =
        users
            .findById(id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário interno não encontrado."));
    if (staff.getRole() == Role.ADMIN) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "A conta mestre não pode ser alterada por esta operação.");
    }
    if (!ASSIGNABLE_ROLES.contains(staff.getRole()) && staff.getRole() != Role.MODERATOR) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Esta não é uma conta interna gerenciável.");
    }
    return staff;
  }

  private void requireAssignableRole(Role role) {
    if (!ASSIGNABLE_ROLES.contains(role)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Escolha uma função interna válida.");
    }
  }

  private String nextStaffCode() {
    for (int attempt = 0; attempt < 100; attempt++) {
      String code = "SU%06d".formatted(RANDOM.nextInt(1_000_000));
      if (!users.existsByStaffCodeIgnoreCase(code)) return code;
    }
    throw new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Não foi possível gerar o código do usuário. Tente novamente.");
  }

  public record PendingOwner(Long id, String name, String email) {}

  public record AuditView(
      Long id,
      String actor,
      String action,
      String entityType,
      String entityId,
      String details,
      Instant createdAt) {
    static AuditView from(AuditLog l) {
      return new AuditView(
          l.getId(),
          l.getActor() == null ? "Sistema" : l.getActor().getName(),
          l.getAction(),
          l.getEntityType(),
          l.getEntityId(),
          l.getDetails(),
          l.getCreatedAt());
    }
  }

  public record CreateStaffRequest(
      @NotBlank @Size(min = 3, max = 120) String name,
      @NotBlank @Email String email,
      @NotBlank @Size(min = 10, max = 72) String password,
      @NotNull Role role) {}

  public record UpdateAppUserRequest(
      @NotBlank @Size(min = 3, max = 120) String name,
      @Size(max = 24) String phone,
      @Size(max = 80) String city,
      @Size(max = 2) String state,
      @NotNull Role role,
      boolean enabled,
      boolean ownerVerified) {}

  public record AppUserView(
      Long id,
      String name,
      String email,
      String phone,
      String city,
      String state,
      String role,
      boolean enabled,
      boolean ownerVerified,
      String photoUrl,
      long listings,
      Instant createdAt) {
    static AppUserView from(UserAccount user, long listings) {
      return new AppUserView(
          user.getId(),
          user.getName(),
          user.getEmail(),
          user.getPhone(),
          user.getCity(),
          user.getState(),
          user.getRole().name(),
          user.isEnabled(),
          user.isOwnerVerified(),
          user.getPhotoUrl(),
          listings,
          user.getCreatedAt());
    }
  }

  public record UpdateManagedSpaceRequest(
      @NotBlank @Size(min = 3, max = 140) String title,
      @NotBlank @Size(min = 20, max = 1200) String description,
      @NotBlank @Size(max = 30) String type,
      @NotNull @DecimalMin("0.10") BigDecimal heightMeters,
      @NotNull @DecimalMin("0.10") BigDecimal lengthMeters,
      @NotNull @DecimalMin("0.10") BigDecimal depthMeters,
      @NotNull @DecimalMin("1.00") BigDecimal price,
      @NotBlank @Size(max = 80) String city,
      @NotBlank @Size(max = 80) String district,
      @NotBlank @Size(max = 9) String postalCode,
      @NotBlank @Size(max = 180) String addressLine,
      @NotBlank @Size(max = 20) String addressNumber,
      @Size(max = 80) String addressComplement,
      @NotBlank @Size(min = 2, max = 2) String state) {}

  public record ManagedSpaceView(
      Long id,
      String title,
      String ownerName,
      String ownerEmail,
      Long ownerId,
      String city,
      String district,
      String postalCode,
      String addressLine,
      String addressNumber,
      String addressComplement,
      String state,
      String type,
      int size,
      BigDecimal heightMeters,
      BigDecimal lengthMeters,
      BigDecimal depthMeters,
      BigDecimal volumeCubicMeters,
      int capacityTotal,
      int capacityUsed,
      BigDecimal price,
      String description,
      boolean available,
      boolean verified,
      String moderationStatus,
      String lifecycleStatus,
      String moderationReason,
      List<String> photos,
      long documentCount,
      String listingDocumentId,
      String listingDocumentStatus,
      String listingDocumentFilename) {
    static ManagedSpaceView from(
        Space space,
        long documentCount,
        String listingDocumentId,
        String listingDocumentStatus,
        String listingDocumentFilename) {
      return new ManagedSpaceView(
          space.getId(),
          space.getTitle(),
          space.getOwner().getName(),
          space.getOwner().getEmail(),
          space.getOwner().getId(),
          space.getCity(),
          space.getDistrict(),
          space.getPostalCode(),
          space.getAddressLine(),
          space.getAddressNumber(),
          space.getAddressComplement(),
          space.getState(),
          space.getType(),
          space.getSize(),
          space.getHeightMeters(),
          space.getLengthMeters(),
          space.getDepthMeters(),
          space.getVolumeCubicMeters(),
          space.getCapacityTotal(),
          space.getCapacityUsed(),
          space.getPrice(),
          space.getDescription(),
          space.isAvailable(),
          space.isVerified(),
          space.getModerationStatus(),
          space.getLifecycleStatus(),
          space.getModerationReason(),
          space.getPhotoUrls(),
          documentCount,
          listingDocumentId,
          listingDocumentStatus,
          listingDocumentFilename);
    }
  }

  public record UpdateStaffRequest(
      @NotBlank @Size(min = 3, max = 120) String name, @NotNull Role role, boolean enabled) {}

  public record ResetPasswordRequest(@NotBlank @Size(min = 10, max = 72) String password) {}

  public record StaffView(
      Long id, String staffCode, String name, String email, Role role, boolean enabled) {
    static StaffView from(UserAccount user) {
      return new StaffView(
          user.getId(),
          user.getStaffCode(),
          user.getName(),
          user.getEmail(),
          user.getRole(),
          user.isEnabled());
    }
  }

  public record RejectSpaceRequest(@NotBlank @Size(min = 10, max = 500) String reason) {}

  public record ModerationSpaceView(
      Long id,
      String title,
      String ownerName,
      String ownerEmail,
      String city,
      String district,
      String type,
      int size,
      BigDecimal price,
      String description,
      List<String> photos,
      String moderationStatus,
      String moderationReason,
      Instant moderatedAt) {
    static ModerationSpaceView from(Space space) {
      return new ModerationSpaceView(
          space.getId(),
          space.getTitle(),
          space.getOwner().getName(),
          space.getOwner().getEmail(),
          space.getCity(),
          space.getDistrict(),
          space.getType(),
          space.getSize(),
          space.getPrice(),
          space.getDescription(),
          space.getPhotoUrls(),
          space.getModerationStatus(),
          space.getModerationReason(),
          space.getModeratedAt());
    }
  }
}
