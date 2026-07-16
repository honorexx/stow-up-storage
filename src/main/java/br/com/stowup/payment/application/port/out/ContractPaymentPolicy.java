package br.com.stowup.payment.application.port.out;

public interface ContractPaymentPolicy {
  void requireApprovedForPayment(String bookingId, String customerEmail);

  boolean confirmBookingAfterApprovedPayment(String bookingId);
}
