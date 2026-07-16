package br.com.stowup.payment.application.port.out;

import br.com.stowup.payment.domain.*;
import java.util.Optional;

public interface PaymentGateway {
  CheckoutSession createCheckout(PaymentOrder order);

  ProviderPayment findPayment(String providerPaymentId);

  Optional<ProviderPayment> findLatestPayment(String externalReference);

  ProviderPayment refundPayment(String providerPaymentId);
}
