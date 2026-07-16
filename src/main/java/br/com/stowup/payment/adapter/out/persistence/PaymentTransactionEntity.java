package br.com.stowup.payment.adapter.out.persistence;

import br.com.stowup.payment.domain.CheckoutSession;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
class PaymentTransactionEntity {
  @Id
  @Column(length = 36)
  private String id;

  @Column(name = "booking_id", nullable = false, length = 20)
  private String bookingId;

  @Column(nullable = false, length = 30)
  private String provider = "MERCADO_PAGO";

  @Column(name = "provider_preference_id", length = 120)
  private String providerPreferenceId;

  @Column(name = "provider_payment_id", length = 120)
  private String providerPaymentId;

  @Column(nullable = false, length = 30)
  private String status;

  @Column(name = "checkout_url", length = 1000)
  private String checkoutUrl;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected PaymentTransactionEntity() {}

  PaymentTransactionEntity(String id, String bookingId, String preferenceId, String checkoutUrl) {
    this.id = id;
    this.bookingId = bookingId;
    this.providerPreferenceId = preferenceId;
    this.checkoutUrl = checkoutUrl;
    this.status = "CHECKOUT_CREATED";
  }

  String getBookingId() {
    return bookingId;
  }

  String getId() {
    return id;
  }

  String getProviderPreferenceId() {
    return providerPreferenceId;
  }

  String getProviderPaymentId() {
    return providerPaymentId;
  }

  String getStatus() {
    return status;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  CheckoutSession checkout() {
    return new CheckoutSession(providerPreferenceId, checkoutUrl);
  }

  boolean canReuse(Instant now) {
    return checkoutUrl != null
        && !checkoutUrl.isBlank()
        && ("CHECKOUT_CREATED".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status))
        && createdAt.isAfter(now.minusSeconds(30 * 60));
  }

  void apply(String providerPaymentId, String status) {
    this.providerPaymentId = providerPaymentId;
    this.status = status;
    this.updatedAt = Instant.now();
  }
}
