package br.com.stowup.account.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface LegalAcceptanceRepository extends JpaRepository<LegalAcceptanceEntity, Long> {
  boolean existsByUserIdAndDocumentCodeAndDocumentVersion(
      Long userId, String documentCode, String documentVersion);
}
