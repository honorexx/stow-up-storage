package br.com.stowup.review;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RentalReviewRepository extends JpaRepository<RentalReview, Long> {
  boolean existsByBookingIdAndReviewerEmailIgnoreCase(String bookingId, String email);

  @EntityGraph(attributePaths = {"reviewer", "reviewedUser", "booking", "space"})
  List<RentalReview> findBySpaceIdAndDirectionOrderByCreatedAtDesc(Long spaceId, String direction);

  @Query(
      "select avg(r.rating) from RentalReview r where r.space.id=:spaceId and r.direction='CUSTOMER_TO_OWNER'")
  Double averageForSpace(@Param("spaceId") Long spaceId);

  long countBySpaceIdAndDirection(Long spaceId, String direction);
}
