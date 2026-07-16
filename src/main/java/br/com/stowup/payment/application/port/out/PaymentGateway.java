package br.com.stowup.payment.application.port.out;

import br.com.stowup.payment.domain.*;

public interface PaymentGateway {
  CheckoutSession createCheckout(PaymentOrder order);

  ProviderPayment findPayment(String providerPaymentId);
}
