package br.com.stowup.account.domain;

import java.time.Instant;
import java.util.UUID;

public record PasswordRecoveryChallenge(
    UUID id,
    Long accountId,
    PasswordRecoveryChannel channel,
    String destination,
    String codeHash,
    String resetTokenHash,
    int failedAttempts,
    Instant expiresAt,
    Instant resendAvailableAt,
    Instant verifiedAt,
    Instant usedAt,
    Instant createdAt) {

  public boolean canResendAt(Instant now) {
    return !resendAvailableAt.isAfter(now);
  }

  public boolean isUsableAt(Instant now) {
    return usedAt == null && expiresAt.isAfter(now) && failedAttempts < 5;
  }

  public PasswordRecoveryChallenge withFailedAttempt() {
    return new PasswordRecoveryChallenge(
        id,
        accountId,
        channel,
        destination,
        codeHash,
        resetTokenHash,
        failedAttempts + 1,
        expiresAt,
        resendAvailableAt,
        verifiedAt,
        usedAt,
        createdAt);
  }

  public PasswordRecoveryChallenge verified(String tokenHash, Instant at) {
    return new PasswordRecoveryChallenge(
        id,
        accountId,
        channel,
        destination,
        codeHash,
        tokenHash,
        failedAttempts,
        expiresAt,
        resendAvailableAt,
        at,
        usedAt,
        createdAt);
  }
}
