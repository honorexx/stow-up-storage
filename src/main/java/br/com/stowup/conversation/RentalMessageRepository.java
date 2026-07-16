package br.com.stowup.conversation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalMessageRepository extends JpaRepository<RentalMessage, Long> {
  List<RentalMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

  Optional<RentalMessage> findFirstByConversationIdOrderByCreatedAtDesc(String conversationId);
}
