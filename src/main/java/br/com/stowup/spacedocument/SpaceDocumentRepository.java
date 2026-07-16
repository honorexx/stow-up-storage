package br.com.stowup.spacedocument;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceDocumentRepository extends JpaRepository<SpaceDocument, String> {
  List<SpaceDocument> findBySpaceIdOrderByUploadedAtDesc(Long spaceId);

  long countBySpaceId(Long spaceId);
}
