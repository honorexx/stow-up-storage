package br.com.stowup.contract;

import br.com.stowup.payment.application.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ContractController {
  private final ContractService service;
  private final PaymentService payments;

  public ContractController(ContractService service, PaymentService payments) {
    this.service = service;
    this.payments = payments;
  }

  @GetMapping("/api/contracts/{bookingId}")
  public ContractView mine(@PathVariable String bookingId, Principal principal) {
    RentalContract contract = service.requireForCustomer(bookingId, principal.getName());
    return ContractView.from(contract, payments.isApproved(bookingId));
  }

  @GetMapping("/api/contracts/{bookingId}/pdf")
  public ResponseEntity<Resource> download(@PathVariable String bookingId, Principal principal) {
    RentalContract c = service.requireForCustomer(bookingId, principal.getName());
    return pdf(service.generatedPath(c), c.getBooking().getContractCode() + ".pdf");
  }

  @PostMapping(
      value = "/api/contracts/{bookingId}/signed",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ContractView upload(
      @PathVariable String bookingId,
      @RequestPart("file") MultipartFile file,
      Principal principal) {
    return ContractView.from(
        service.uploadSigned(bookingId, principal.getName(), file), payments.isApproved(bookingId));
  }

  @GetMapping("/api/staff/contracts")
  @Transactional(readOnly = true)
  public List<StaffContractView> queue(
      @RequestParam(defaultValue = "PENDING_REVIEW") String status) {
    return service.queue(status).stream().map(StaffContractView::from).toList();
  }

  @GetMapping("/api/staff/contracts/{id}/signed")
  public ResponseEntity<Resource> signed(@PathVariable String id) {
    RentalContract c = service.requireStaff(id);
    return pdf(service.signedPath(c), c.getOriginalSignedFilename());
  }

  @PostMapping("/api/staff/contracts/{id}/review")
  public StaffContractView review(
      @PathVariable String id, @Valid @RequestBody ReviewRequest request, Principal principal) {
    return StaffContractView.from(
        service.review(
            id,
            principal.getName(),
            request.result(),
            request.signerNameMatches(),
            request.validatorReportReference(),
            request.notes()));
  }

  private ResponseEntity<Resource> pdf(java.nio.file.Path path, String filename) {
    Resource resource = new FileSystemResource(path);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(filename).build().toString())
        .body(resource);
  }

  public record ReviewRequest(
      @NotBlank @Pattern(regexp = "VALID|INVALID") String result,
      boolean signerNameMatches,
      @NotBlank @Size(max = 500) String validatorReportReference,
      @NotBlank @Size(min = 5, max = 1000) String notes) {}

  public record ContractView(
      String id,
      String bookingId,
      String contractCode,
      String status,
      boolean paymentApproved,
      String originalFilename,
      String validatorReportReference,
      String reviewNotes,
      Instant createdAt,
      Instant signedAt,
      Instant reviewedAt) {
    static ContractView from(RentalContract c, boolean paymentApproved) {
      return new ContractView(
          c.getId(),
          c.getBooking().getId(),
          c.getBooking().getContractCode(),
          c.getStatus(),
          paymentApproved,
          c.getOriginalSignedFilename(),
          c.getValidatorReportReference(),
          c.getReviewNotes(),
          c.getCreatedAt(),
          c.getSignedAt(),
          c.getReviewedAt());
    }
  }

  public record StaffContractView(
      String id,
      String bookingId,
      String contractCode,
      String status,
      String customerName,
      String customerEmail,
      String ownerName,
      String spaceTitle,
      String originalFilename,
      String validatorUrl,
      Instant signedAt) {
    static StaffContractView from(RentalContract c) {
      return new StaffContractView(
          c.getId(),
          c.getBooking().getId(),
          c.getBooking().getContractCode(),
          c.getStatus(),
          c.getBooking().getCustomer().getName(),
          c.getBooking().getCustomer().getEmail(),
          c.getBooking().getSpace().getOwner().getName(),
          c.getBooking().getSpace().getTitle(),
          c.getOriginalSignedFilename(),
          "https://validar.iti.gov.br/",
          c.getSignedAt());
    }
  }
}
