package br.com.stowup.booking;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, String> {
  List<Booking> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

  List<Booking> findBySpaceOwnerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

  long countByStatus(BookingStatus status);

  long countBySpaceOwnerEmailIgnoreCaseAndStatus(String email, BookingStatus status);

  List<Booking> findBySpaceIdAndCustomerIdAndStartDateAndMonthsAndStatusInOrderByCreatedAtDesc(
      Long spaceId,
      Long customerId,
      LocalDate startDate,
      int months,
      Collection<BookingStatus> statuses);

  @Query(
      "select (count(b) > 0) from Booking b where b.space.id=:spaceId and b.status in (br.com.stowup.booking.BookingStatus.PENDING,br.com.stowup.booking.BookingStatus.CONFIRMED) and b.startDate < :end and b.endDate > :start")
  boolean hasConflict(
      @Param("spaceId") Long spaceId, @Param("start") LocalDate start, @Param("end") LocalDate end);

  @Query(
      "select b from Booking b where b.space.id=:spaceId and b.status in (br.com.stowup.booking.BookingStatus.PENDING,br.com.stowup.booking.BookingStatus.CONFIRMED) order by b.startDate")
  List<Booking> availability(@Param("spaceId") Long spaceId);
}
