package br.com.stowup.listingdocument.adapter.out.persistence;

import br.com.stowup.listingdocument.domain.ListingDocumentStatus;
import br.com.stowup.listingdocument.domain.OwnerListingDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerListingDocumentRepository
    extends JpaRepository<OwnerListingDocument, String> {
  @EntityGraph(attributePaths = {"space", "space.owner"})
  Optional<OwnerListingDocument> findBySpaceId(Long spaceId);

  @EntityGraph(attributePaths = {"space", "space.owner"})
  List<OwnerListingDocument> findByStatusOrderByCreatedAtAsc(ListingDocumentStatus status);

  @Override
  @EntityGraph(attributePaths = {"space", "space.owner"})
  Optional<OwnerListingDocument> findById(String id);
}
