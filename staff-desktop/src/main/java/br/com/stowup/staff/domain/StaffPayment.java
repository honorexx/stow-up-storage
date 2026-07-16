package br.com.stowup.staff.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record StaffPayment(
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
  @Override
  public String toString() {
    return bookingId
        + " · "
        + statusLabel()
        + "\n"
        + customerName
        + " · "
        + spaceTitle
        + " · R$ "
        + amount;
  }

  public String statusLabel() {
    if ("approved".equalsIgnoreCase(status)) return "Aprovado";
    if ("refunded".equalsIgnoreCase(status)) return "Estornado";
    if ("pending".equalsIgnoreCase(status)) return "Pendente";
    if ("CHECKOUT_CREATED".equalsIgnoreCase(status)) return "Checkout criado";
    if ("CHECKOUT_FAILED".equalsIgnoreCase(status)) return "Checkout expirado";
    return status;
  }
}
