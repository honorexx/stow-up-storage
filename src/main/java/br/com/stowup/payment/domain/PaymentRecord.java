package br.com.stowup.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRecord(
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
    Instant updatedAt) {
  public boolean receiptAvailable() {
    return providerPaymentId != null
        && !providerPaymentId.isBlank()
        && ("approved".equalsIgnoreCase(status) || "refunded".equalsIgnoreCase(status));
  }
}
