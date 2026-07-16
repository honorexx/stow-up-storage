package br.com.stowup.conversation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalConversationRepository extends JpaRepository<RentalConversation, String> {
  List<RentalConversation> findByCustomerEmailIgnoreCaseOrOwnerEmailIgnoreCaseOrderByUpdatedAtDesc(
      String customerEmail, String ownerEmail);

  Optional<RentalConversation> findBySpaceIdAndCustomerEmailIgnoreCase(
      Long spaceId, String customerEmail);

  @Query(
      "select c from RentalConversation c where c.id = :id and (lower(c.customer.email) = lower(:email) or lower(c.owner.email) = lower(:email))")
  Optional<RentalConversation> findAccessible(@Param("id") String id, @Param("email") String email);
}
