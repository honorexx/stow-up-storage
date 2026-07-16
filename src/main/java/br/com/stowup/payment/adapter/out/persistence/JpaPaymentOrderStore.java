package br.com.stowup.payment.adapter.out.persistence;

import br.com.stowup.booking.*;
import br.com.stowup.payment.application.port.out.PaymentOrderStore;
import br.com.stowup.payment.domain.*;
import br.com.stowup.space.SpaceRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class JpaPaymentOrderStore implements PaymentOrderStore {
  private final BookingRepository bookings;
  private final PaymentTransactionRepository transactions;
  private final SpaceRepository spaces;

  public JpaPaymentOrderStore(
      BookingRepository bookings,
      PaymentTransactionRepository transactions,
      SpaceRepository spaces) {
    this.bookings = bookings;
    this.transactions = transactions;
    this.spaces = spaces;
  }

  @Override
  @Transactional
  public PaymentOrder requireOwnedBooking(String bookingId, String customerEmail) {
    Booking booking =
        bookings
            .findByIdForUpdate(bookingId)
            .filter(item -> item.getCustomer().getEmail().equalsIgnoreCase(customerEmail))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada."));
    if (booking.getStatus() != BookingStatus.PENDING)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Esta reserva não aceita um novo pagamento.");
    spaces
        .findByIdForBooking(booking.getSpace().getId())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espaço não encontrado."));
    if (bookings.hasConfirmedConflictExcluding(
        booking.getId(),
        booking.getSpace().getId(),
        booking.getStartDate(),
        booking.getEndDate())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Este período acabou de ser reservado por outro cliente. Nenhuma cobrança foi realizada.");
    }
    return new PaymentOrder(
        booking.getId(),
        "Locação StowUp - " + booking.getSpace().getTitle(),
        booking.getTotal(),
        booking.getCustomer().getName(),
        booking.getCustomer().getEmail());
  }

  @Override
  @Transactional(readOnly = true)
  public void requireOwnership(String bookingId, String customerEmail) {
    bookings
        .findById(bookingId)
        .filter(item -> item.getCustomer().getEmail().equalsIgnoreCase(customerEmail))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada."));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CheckoutSession> findReusableCheckout(String bookingId) {
    return transactions
        .findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
        .filter(transaction -> transaction.canReuse(Instant.now()))
        .map(PaymentTransactionEntity::checkout);
  }

  @Override
  @Transactional
  public void recordCheckout(String bookingId, CheckoutSession checkout) {
    transactions.save(
        new PaymentTransactionEntity(
            UUID.randomUUID().toString(),
            bookingId,
            checkout.preferenceId(),
            checkout.checkoutUrl()));
  }

  @Override
  @Transactional
  public void applyProviderPayment(ProviderPayment payment) {
    Booking booking =
        bookings
            .findById(payment.externalReference())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pagamento sem reserva correspondente."));
    if (payment.amount() == null
        || booking.getTotal().compareTo(payment.amount()) != 0
        || !"BRL".equalsIgnoreCase(payment.currency())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O valor ou a moeda do pagamento não confere com a reserva.");
    }
    PaymentTransactionEntity transaction =
        transactions
            .findFirstByBookingIdOrderByCreatedAtDesc(payment.externalReference())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pagamento sem reserva correspondente."));
    transaction.apply(payment.paymentId(), payment.status());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isApproved(String bookingId) {
    return transactions.existsByBookingIdAndStatusIgnoreCase(bookingId, "approved");
  }
}
