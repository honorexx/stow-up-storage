package br.com.stowup.payment.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, String> {
  Optional<PaymentTransactionEntity> findFirstByBookingIdOrderByCreatedAtDesc(String bookingId);

  boolean existsByBookingIdAndStatusIgnoreCase(String bookingId, String status);
}
