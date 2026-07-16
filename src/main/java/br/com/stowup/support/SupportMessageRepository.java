package br.com.stowup.support;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
  List<SupportMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
