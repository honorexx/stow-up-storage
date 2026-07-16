package br.com.stowup.payment.application.port.out;

import br.com.stowup.payment.domain.PaymentRecord;

public interface PaymentReceiptGenerator {
  byte[] generate(PaymentRecord payment);
}
