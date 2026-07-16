package br.com.stowup.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import br.com.stowup.payment.application.PaymentService;
import br.com.stowup.payment.application.port.out.ContractPaymentPolicy;
import br.com.stowup.payment.application.port.out.PaymentGateway;
import br.com.stowup.payment.application.port.out.PaymentOrderStore;
import br.com.stowup.payment.domain.CheckoutSession;
import br.com.stowup.payment.domain.PaymentOrder;
import br.com.stowup.payment.domain.ProviderPayment;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {
  @Test
  void reusesRecentCheckoutInsteadOfCreatingDuplicatePreference() {
    PaymentGateway gateway = mock(PaymentGateway.class);
    PaymentOrderStore orders = mock(PaymentOrderStore.class);
    ContractPaymentPolicy contracts = mock(ContractPaymentPolicy.class);
    PaymentService service = new PaymentService(gateway, orders, contracts);
    PaymentOrder order =
        new PaymentOrder(
            "RSV-1", "Locação", new BigDecimal("108.00"), "Cliente", "cliente@test.com");
    CheckoutSession existing = new CheckoutSession("PREF-1", "https://checkout.test/1");
    when(orders.requireOwnedBooking("RSV-1", "cliente@test.com")).thenReturn(order);
    when(orders.findReusableCheckout("RSV-1")).thenReturn(Optional.of(existing));

    assertThat(service.createCheckout("RSV-1", "cliente@test.com")).isEqualTo(existing);
    verifyNoInteractions(gateway);
    verify(orders, never()).recordCheckout(anyString(), any());
  }

  @Test
  void reconcilesApprovedPaymentWhenWebhookCannotReachLocalBackend() {
    PaymentGateway gateway = mock(PaymentGateway.class);
    PaymentOrderStore orders = mock(PaymentOrderStore.class);
    ContractPaymentPolicy contracts = mock(ContractPaymentPolicy.class);
    PaymentService service = new PaymentService(gateway, orders, contracts);
    PaymentOrder order =
        new PaymentOrder(
            "RSV-1", "Locação", new BigDecimal("108.00"), "Cliente", "cliente@test.com");
    ProviderPayment payment =
        new ProviderPayment("PAY-1", "RSV-1", "approved", new BigDecimal("108.00"), "BRL");
    when(gateway.findLatestPayment("RSV-1")).thenReturn(Optional.of(payment));
    when(contracts.confirmBookingAfterApprovedPayment("RSV-1")).thenReturn(true);
    when(orders.isApproved("RSV-1")).thenReturn(true);

    assertThat(service.reconcile("RSV-1", "cliente@test.com")).isTrue();
    verify(orders).requireOwnership("RSV-1", "cliente@test.com");
    verify(orders).applyProviderPayment(payment);
    verify(contracts).confirmBookingAfterApprovedPayment("RSV-1");
  }

  @Test
  void refundsApprovedPaymentWhenAnotherCustomerAlreadyWonThePeriod() {
    PaymentGateway gateway = mock(PaymentGateway.class);
    PaymentOrderStore orders = mock(PaymentOrderStore.class);
    ContractPaymentPolicy contracts = mock(ContractPaymentPolicy.class);
    PaymentService service = new PaymentService(gateway, orders, contracts);
    ProviderPayment approved =
        new ProviderPayment("PAY-2", "RSV-2", "approved", new BigDecimal("108.00"), "BRL");
    ProviderPayment refunded =
        new ProviderPayment("PAY-2", "RSV-2", "refunded", new BigDecimal("108.00"), "BRL");
    when(gateway.findPayment("PAY-2")).thenReturn(approved);
    when(contracts.confirmBookingAfterApprovedPayment("RSV-2")).thenReturn(false);
    when(gateway.refundPayment("PAY-2")).thenReturn(refunded);

    service.receiveProviderNotification("PAY-2");

    verify(orders).applyProviderPayment(approved);
    verify(gateway).refundPayment("PAY-2");
    verify(orders).applyProviderPayment(refunded);
  }
}
