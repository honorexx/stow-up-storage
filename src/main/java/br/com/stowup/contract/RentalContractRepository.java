package br.com.stowup.contract;

import java.util.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalContractRepository extends JpaRepository<RentalContract, String> {
  @EntityGraph(
      attributePaths = {"booking", "booking.customer", "booking.space", "booking.space.owner"})
  Optional<RentalContract> findByBookingId(String bookingId);

  @EntityGraph(
      attributePaths = {"booking", "booking.customer", "booking.space", "booking.space.owner"})
  Optional<RentalContract> findByBookingIdAndBookingCustomerEmailIgnoreCase(
      String bookingId, String email);

  @EntityGraph(
      attributePaths = {"booking", "booking.customer", "booking.space", "booking.space.owner"})
  List<RentalContract> findAllByOrderByCreatedAtDesc();

  @EntityGraph(
      attributePaths = {"booking", "booking.customer", "booking.space", "booking.space.owner"})
  List<RentalContract> findByStatusOrderByCreatedAtDesc(String status);

  long countByStatus(String status);

  @Override
  @EntityGraph(
      attributePaths = {"booking", "booking.customer", "booking.space", "booking.space.owner"})
  Optional<RentalContract> findById(String id);
}
