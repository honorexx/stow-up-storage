package br.com.stowup.payment.application.port.out;

import br.com.stowup.payment.domain.PaymentRecord;
import java.util.List;

public interface PaymentRecordStore {
  List<PaymentRecord> findAll();

  PaymentRecord require(String id);
}
