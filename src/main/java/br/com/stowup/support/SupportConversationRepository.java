package br.com.stowup.support;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportConversationRepository extends JpaRepository<SupportConversation, String> {
  Optional<SupportConversation> findFirstByCustomerEmailIgnoreCaseOrderByUpdatedAtDesc(
      String email);

  java.util.List<SupportConversation> findAllByOrderByUpdatedAtDesc();

  List<SupportConversation> findByCustomerEmailIgnoreCaseOrderByUpdatedAtDesc(String email);

  Optional<SupportConversation> findByIdAndCustomerEmailIgnoreCase(String id, String email);

  long countByStatus(String status);

  long countByCategoryAndStatus(SupportCategory category, String status);
}
