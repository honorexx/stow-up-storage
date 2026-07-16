package br.com.stowup.account.adapter.out.persistence;

import br.com.stowup.account.domain.PasswordRecoveryChallenge;
import br.com.stowup.account.domain.PasswordRecoveryChannel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_recovery_requests")
class PasswordRecoveryEntity {
  @Id private UUID id;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private PasswordRecoveryChannel channel;

  @Column(nullable = false, length = 180)
  private String destination;

  @Column(name = "code_hash", nullable = false)
  private String codeHash;

  @Column(name = "reset_token_hash")
  private String resetTokenHash;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "resend_available_at", nullable = false)
  private Instant resendAvailableAt;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected PasswordRecoveryEntity() {}

  static PasswordRecoveryEntity from(PasswordRecoveryChallenge source) {
    PasswordRecoveryEntity entity = new PasswordRecoveryEntity();
    entity.id = source.id();
    entity.accountId = source.accountId();
    entity.channel = source.channel();
    entity.destination = source.destination();
    entity.codeHash = source.codeHash();
    entity.resetTokenHash = source.resetTokenHash();
    entity.failedAttempts = source.failedAttempts();
    entity.expiresAt = source.expiresAt();
    entity.resendAvailableAt = source.resendAvailableAt();
    entity.verifiedAt = source.verifiedAt();
    entity.usedAt = source.usedAt();
    entity.createdAt = source.createdAt();
    return entity;
  }

  PasswordRecoveryChallenge toDomain() {
    return new PasswordRecoveryChallenge(
        id,
        accountId,
        channel,
        destination,
        codeHash,
        resetTokenHash,
        failedAttempts,
        expiresAt,
        resendAvailableAt,
        verifiedAt,
        usedAt,
        createdAt);
  }
}
