package br.com.stowup.payment.adapter.in.web;

import br.com.stowup.payment.application.PaymentAdministrationService;
import br.com.stowup.payment.domain.PaymentRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {
  private final PaymentAdministrationService payments;

  public AdminPaymentController(PaymentAdministrationService payments) {
    this.payments = payments;
  }

  @GetMapping
  public List<PaymentView> list() {
    return payments.list().stream().map(PaymentView::from).toList();
  }

  @GetMapping(value = "/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> receipt(@PathVariable String id) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"comprovante-stowup-" + id + ".pdf\"")
        .body(payments.receipt(id));
  }

  public record PaymentView(
      String id,
      String bookingId,
      String providerPreferenceId,
      String providerPaymentId,
      String status,
      BigDecimal amount,
      String customerName,
      String customerEmail,
      String spaceTitle,
      Instant createdAt,
      Instant updatedAt,
      boolean receiptAvailable) {
    static PaymentView from(PaymentRecord payment) {
      return new PaymentView(
          payment.id(),
          payment.bookingId(),
          payment.providerPreferenceId(),
          payment.providerPaymentId(),
          payment.status(),
          payment.amount(),
          payment.customerName(),
          payment.customerEmail(),
          payment.spaceTitle(),
          payment.createdAt(),
          payment.updatedAt(),
          payment.receiptAvailable());
    }
  }
}
