package br.com.stowup.payment.application.port.out;

import br.com.stowup.payment.domain.*;
import java.util.Optional;

public interface PaymentOrderStore {
  PaymentOrder requireOwnedBooking(String bookingId, String customerEmail);

  Optional<CheckoutSession> findReusableCheckout(String bookingId);

  void recordCheckout(String bookingId, CheckoutSession checkout);

  void applyProviderPayment(ProviderPayment payment);

  boolean isApproved(String bookingId);
}
