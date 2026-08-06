package br.com.stowup.contract;

import br.com.stowup.audit.AuditService;
import br.com.stowup.booking.*;
import br.com.stowup.contract.application.port.out.ContractDocumentGenerator;
import br.com.stowup.contract.domain.ContractData;
import br.com.stowup.notification.DocumentEmailService;
import br.com.stowup.notification.NotificationService;
import br.com.stowup.payment.application.port.out.ContractPaymentPolicy;
import br.com.stowup.space.Space;
import br.com.stowup.user.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContractService implements ContractPaymentPolicy {
  private final RentalContractRepository contracts;
  private final BookingRepository bookings;
  private final br.com.stowup.space.SpaceRepository spaces;
  private final UserRepository users;
  private final ContractDocumentGenerator documents;
  private final Path contractRoot;
  private final NotificationService notifications;
  private final AuditService audit;
  private final DocumentEmailService documentEmails;

  public ContractService(
      RentalContractRepository contracts,
      BookingRepository bookings,
      br.com.stowup.space.SpaceRepository spaces,
      UserRepository users,
      ContractDocumentGenerator documents,
      NotificationService notifications,
      DocumentEmailService documentEmails,
      AuditService audit,
      @Value("${stowup.uploads.directory}") String uploadDirectory) {
    this.contracts = contracts;
    this.bookings = bookings;
    this.spaces = spaces;
    this.users = users;
    this.documents = documents;
    this.notifications = notifications;
    this.documentEmails = documentEmails;
    this.audit = audit;
    this.contractRoot = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("contracts");
  }

  @Transactional
  public RentalContract requireForCustomer(String bookingId, String email) {
    return contracts
        .findByBookingIdAndBookingCustomerEmailIgnoreCase(bookingId, email)
        .orElseGet(() -> generate(bookingId, email));
  }

  private RentalContract generate(String bookingId, String email) {
    Booking booking =
        bookings
            .findById(bookingId)
            .filter(item -> item.getCustomer().getEmail().equalsIgnoreCase(email))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada."));
    Path path = generateDocument(booking);
    RentalContract contract = contracts.save(new RentalContract(booking, path.toString()));
    documentEmails.sendPdf(
        booking.getCustomer().getEmail(),
        "Contrato da sua reserva StowUp",
        "Olá, "
            + booking.getCustomer().getName()
            + ".\n\nSeu contrato está anexado. Confira os dados, assine pelo gov.br e envie o PDF assinado pelo aplicativo StowUp.",
        path,
        booking.getContractCode() + ".pdf");
    return contract;
  }

  @Transactional
  public RentalContract uploadSigned(String bookingId, String email, MultipartFile file) {
    RentalContract contract = requireForCustomer(bookingId, email);
    if (file.isEmpty() || file.getSize() > 15 * 1024 * 1024)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Envie o PDF assinado com até 15 MB.");
    if (!"application/pdf".equalsIgnoreCase(file.getContentType()) || !hasPdfHeader(file))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O arquivo precisa ser o PDF original assinado eletronicamente.");
    try {
      Files.createDirectories(contractRoot);
      Path target = contractRoot.resolve(contract.getId() + "-signed.pdf");
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
      contract.signed(target.toString(), safeFilename(file.getOriginalFilename()));
      return contract;
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível guardar o contrato assinado.", e);
    }
  }

  @Transactional(readOnly = true)
  public List<RentalContract> queue(String status) {
    return "ALL".equalsIgnoreCase(status)
        ? contracts.findAllByOrderByCreatedAtDesc()
        : contracts.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
  }

  @Transactional(readOnly = true)
  public RentalContract requireStaff(String id) {
    return contracts
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
  }

  @Transactional
  public RentalContract review(
      String id,
      String reviewerEmail,
      String result,
      boolean signerNameMatches,
      String validatorReference,
      String notes) {
    RentalContract contract = requireStaff(id);
    if (!"PENDING_REVIEW".equals(contract.getStatus()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Este contrato não está aguardando revisão.");
    UserAccount reviewer =
        users
            .findByEmailIgnoreCase(reviewerEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if ("VALID".equals(result) && signerNameMatches) {
      contract.approve(reviewer, validatorReference, notes);
    } else contract.reject(reviewer, signerNameMatches, validatorReference, notes);
    boolean approved = "APPROVED".equals(contract.getStatus());
    notifications.send(
        contract.getBooking().getCustomer(),
        approved ? "CONTRACT_APPROVED" : "CONTRACT_REJECTED",
        approved ? "Contrato aprovado" : "Contrato precisa de correção",
        approved ? "O pagamento da reserva já foi liberado." : notes,
        "CONTRACT",
        contract.getBooking().getId());
    audit.record(
        reviewerEmail,
        approved ? "CONTRACT_APPROVED" : "CONTRACT_REJECTED",
        "CONTRACT",
        contract.getId(),
        notes);
    return contract;
  }

  @Transactional(readOnly = true)
  @Override
  public void requireApprovedForPayment(String bookingId, String customerEmail) {
    RentalContract contract =
        contracts
            .findByBookingIdAndBookingCustomerEmailIgnoreCase(bookingId, customerEmail)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Assine e envie o contrato antes de iniciar o pagamento."));
    if (!"APPROVED".equals(contract.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O pagamento será liberado após a aprovação do contrato assinado.");
    }
  }

  @Transactional
  @Override
  public boolean confirmBookingAfterApprovedPayment(String bookingId) {
    RentalContract contract =
        contracts
            .findByBookingId(bookingId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT, "Contrato da reserva não encontrado."));
    if (!"APPROVED".equals(contract.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O contrato precisa estar aprovado antes da confirmação do pagamento.");
    }
    Booking booking =
        bookings
            .findByIdForUpdate(bookingId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada."));
    spaces
        .findByIdForBooking(booking.getSpace().getId())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espaço não encontrado."));
    if (booking.getStatus() == BookingStatus.CONFIRMED
        || booking.getStatus() == BookingStatus.COMPLETED) return true;
    if (booking.getStatus() == BookingStatus.CANCELLED) return false;
    if (booking.getStatus() != BookingStatus.PENDING) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A reserva não está aguardando pagamento.");
    }
    if (bookings.hasConfirmedConflictExcluding(
        booking.getId(),
        booking.getSpace().getId(),
        booking.getStartDate(),
        booking.getEndDate())) {
      booking.cancel();
      notifications.send(
          booking.getCustomer(),
          "BOOKING_LOST",
          "Espaço reservado por outro cliente",
          "Outro cliente concluiu o pagamento antes. O valor desta tentativa será estornado.",
          "BOOKING",
          bookingId);
      audit.record(
          null,
          "BOOKING_PAYMENT_CONFLICT",
          "BOOKING",
          bookingId,
          "Reserva cancelada e pagamento encaminhado para estorno");
      return false;
    }
    booking.setStatus(BookingStatus.CONFIRMED);
    booking.getSpace().occupy(booking.getReservedCapacity());
    for (Booking pending :
        bookings.findOverlappingPending(
            booking.getId(),
            booking.getSpace().getId(),
            booking.getStartDate(),
            booking.getEndDate())) {
      pending.cancel();
      notifications.send(
          pending.getCustomer(),
          "BOOKING_UNAVAILABLE",
          "Período não está mais disponível",
          booking.getSpace().getTitle()
              + " foi confirmado por outro cliente. Sua solicitação foi cancelada sem cobrança.",
          "BOOKING",
          pending.getId());
    }
    notifications.send(
        booking.getCustomer(),
        "BOOKING_CONFIRMED",
        "Reserva confirmada",
        "Pagamento aprovado e contrato ativo.",
        "BOOKING",
        bookingId);
    notifications.send(
        booking.getSpace().getOwner(),
        "BOOKING_CONFIRMED",
        "Nova locação confirmada",
        booking.getSpace().getTitle() + " foi reservado.",
        "BOOKING",
        bookingId);
    audit.record(
        null, "PAYMENT_APPROVED", "BOOKING", bookingId, "Reserva confirmada automaticamente");
    return true;
  }

  public Path generatedPath(RentalContract contract) {
    return "GENERATED".equals(contract.getStatus())
        ? generateDocument(contract.getBooking())
        : Path.of(contract.getGeneratedPath());
  }

  public Path signedPath(RentalContract contract) {
    if (contract.getSignedPath() == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF assinado ainda não enviado.");
    return Path.of(contract.getSignedPath());
  }

  private static boolean hasPdfHeader(MultipartFile file) {
    try (InputStream in = file.getInputStream()) {
      return new String(in.readNBytes(5), java.nio.charset.StandardCharsets.US_ASCII)
          .equals("%PDF-");
    } catch (IOException e) {
      return false;
    }
  }

  private static String safeFilename(String value) {
    return value == null
        ? "contrato-assinado.pdf"
        : Path.of(value).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }

  private Path generateDocument(Booking booking) {
    Space space = booking.getSpace();
    UserAccount owner = space.getOwner();
    UserAccount customer = booking.getCustomer();
    String address =
        String.join(
            ", ",
            safe(space.getAddressLine()),
            safe(space.getAddressNumber()),
            safe(space.getDistrict()),
            safe(space.getCity()) + "/" + safe(space.getState()));
    return documents.generate(
        new ContractData(
            booking.getId(),
            booking.getContractCode(),
            owner.getName(),
            owner.getEmail(),
            customer.getName(),
            customer.getEmail(),
            space.getTitle(),
            space.getType(),
            space.getSize(),
            address,
            booking.getStartDate(),
            booking.getMonths(),
            booking.getTotal()));
  }
}
