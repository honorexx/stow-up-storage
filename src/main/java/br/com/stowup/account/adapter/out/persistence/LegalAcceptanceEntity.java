package br.com.stowup.account.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "legal_acceptances")
class LegalAcceptanceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "document_code", nullable = false, length = 40)
  private String documentCode;

  @Column(name = "document_version", nullable = false, length = 30)
  private String documentVersion;

  @Column(name = "accepted_at", nullable = false, updatable = false)
  private Instant acceptedAt = Instant.now();

  protected LegalAcceptanceEntity() {}

  LegalAcceptanceEntity(Long userId, String documentCode, String documentVersion) {
    this.userId = userId;
    this.documentCode = documentCode;
    this.documentVersion = documentVersion;
  }
}
