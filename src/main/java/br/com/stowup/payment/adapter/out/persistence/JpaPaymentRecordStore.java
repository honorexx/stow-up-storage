package br.com.stowup.payment.adapter.out.persistence;

import br.com.stowup.booking.Booking;
import br.com.stowup.booking.BookingRepository;
import br.com.stowup.payment.application.port.out.PaymentRecordStore;
import br.com.stowup.payment.domain.PaymentRecord;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
class JpaPaymentRecordStore implements PaymentRecordStore {
  private final PaymentTransactionRepository transactions;
  private final BookingRepository bookings;

  JpaPaymentRecordStore(PaymentTransactionRepository transactions, BookingRepository bookings) {
    this.transactions = transactions;
    this.bookings = bookings;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PaymentRecord> findAll() {
    return transactions.findAllByOrderByCreatedAtDesc().stream().map(this::map).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PaymentRecord require(String id) {
    return transactions
        .findById(id)
        .map(this::map)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento não encontrado."));
  }

  private PaymentRecord map(PaymentTransactionEntity transaction) {
    Booking booking =
        bookings
            .findById(transaction.getBookingId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada."));
    return new PaymentRecord(
        transaction.getId(),
        transaction.getBookingId(),
        transaction.getProviderPreferenceId(),
        transaction.getProviderPaymentId(),
        transaction.getStatus(),
        booking.getTotal(),
        booking.getCustomer().getName(),
        booking.getCustomer().getEmail(),
        booking.getSpace().getTitle(),
        transaction.getCreatedAt(),
        transaction.getUpdatedAt());
  }
}
