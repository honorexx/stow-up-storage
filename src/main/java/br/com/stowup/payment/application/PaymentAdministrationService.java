package br.com.stowup.payment.application;

import br.com.stowup.payment.application.port.out.PaymentReceiptGenerator;
import br.com.stowup.payment.application.port.out.PaymentRecordStore;
import br.com.stowup.payment.domain.PaymentRecord;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PaymentAdministrationService {
  private final PaymentRecordStore records;
  private final PaymentReceiptGenerator receipts;

  public PaymentAdministrationService(
      PaymentRecordStore records, PaymentReceiptGenerator receipts) {
    this.records = records;
    this.receipts = receipts;
  }

  public List<PaymentRecord> list() {
    return records.findAll();
  }

  public byte[] receipt(String id) {
    PaymentRecord payment = records.require(id);
    if (!payment.receiptAvailable())
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT,
          "O comprovante fica disponível após aprovação ou estorno do pagamento.");
    return receipts.generate(payment);
  }
}
