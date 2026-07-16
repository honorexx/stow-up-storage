package br.com.stowup.space;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceRepository extends JpaRepository<Space, Long> {
  List<Space> findByAvailableTrue();

  @EntityGraph(attributePaths = {"owner", "photos"})
  List<Space> findAllByOrderByIdDesc();

  @EntityGraph(attributePaths = {"owner", "photos"})
  List<Space> findByAvailableTrueAndVerifiedTrue();

  List<Space> findByOwnerEmailIgnoreCaseOrderByIdDesc(String email);

  Optional<Space> findByIdAndOwnerEmailIgnoreCase(Long id, String email);

  Optional<Space> findByIdAndAvailableTrueAndVerifiedTrue(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Space s join fetch s.owner where s.id = :id")
  Optional<Space> findByIdForBooking(@Param("id") Long id);

  long countByAvailableTrue();

  long countByLifecycleStatus(String status);

  long countByOwnerEmailIgnoreCaseAndLifecycleStatus(String email, String status);

  long countByOwnerId(Long ownerId);

  List<Space> findByModerationStatusOrderByIdDesc(String moderationStatus);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update Space s set s.available = false, s.verified = false, s.moderationStatus = 'WITHDRAWN', s.moderationReason = 'Conta encerrada' where s.owner.id = :ownerId")
  int withdrawAllByOwnerId(@Param("ownerId") Long ownerId);
}
