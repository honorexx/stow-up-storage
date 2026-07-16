package br.com.stowup.payment.application;

import br.com.stowup.payment.application.port.out.*;
import br.com.stowup.payment.domain.*;
import org.springframework.stereotype.Service;

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

  public void receiveProviderNotification(String providerPaymentId) {
    ProviderPayment payment = gateway.findPayment(providerPaymentId);
    orders.applyProviderPayment(payment);
    if ("approved".equalsIgnoreCase(payment.status()))
      contracts.confirmBookingAfterApprovedPayment(payment.externalReference());
  }

  public boolean isApproved(String bookingId) {
    return orders.isApproved(bookingId);
  }
}
