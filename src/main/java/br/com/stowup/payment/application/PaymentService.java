package br.com.stowup.payment.application;

import br.com.stowup.payment.application.port.out.*;
import br.com.stowup.payment.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
  private final PaymentGateway gateway;
  private final PaymentOrderStore orders;
  private final ContractPaymentPolicy contracts;

  public PaymentService(
      PaymentGateway gateway, PaymentOrderStore orders, ContractPaymentPolicy contracts) {
    this.gateway = gateway;
    this.orders = orders;
    this.contracts = contracts;
  }

  public CheckoutSession createCheckout(String bookingId, String customerEmail) {
    contracts.requireApprovedForPayment(bookingId, customerEmail);
    PaymentOrder order = orders.requireOwnedBooking(bookingId, customerEmail);
    var reusable = orders.findReusableCheckout(bookingId);
    if (reusable.isPresent()) return reusable.get();
    CheckoutSession checkout = gateway.createCheckout(order);
    orders.recordCheckout(bookingId, checkout);
    return checkout;
  }

  @Transactional
  public void receiveProviderNotification(String providerPaymentId) {
    ProviderPayment payment = gateway.findPayment(providerPaymentId);
    apply(payment);
  }

  @Transactional
  public boolean reconcile(String bookingId, String customerEmail) {
    orders.requireOwnership(bookingId, customerEmail);
    gateway.findLatestPayment(bookingId).ifPresent(this::apply);
    return orders.isApproved(bookingId);
  }

  public boolean isApproved(String bookingId) {
    return orders.isApproved(bookingId);
  }

  protected void apply(ProviderPayment payment) {
    orders.applyProviderPayment(payment);
    if ("approved".equalsIgnoreCase(payment.status())
        && !contracts.confirmBookingAfterApprovedPayment(payment.externalReference())) {
      ProviderPayment refunded = gateway.refundPayment(payment.paymentId());
      orders.applyProviderPayment(refunded);
    }
  }
}
