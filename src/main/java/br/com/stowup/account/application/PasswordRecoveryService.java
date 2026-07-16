package br.com.stowup.account.application;

import br.com.stowup.account.application.port.out.AccountStore;
import br.com.stowup.account.application.port.out.PasswordHasher;
import br.com.stowup.account.application.port.out.PasswordRecoveryMessageSender;
import br.com.stowup.account.application.port.out.PasswordRecoveryStore;
import br.com.stowup.account.domain.Account;
import br.com.stowup.account.domain.PasswordRecoveryChallenge;
import br.com.stowup.account.domain.PasswordRecoveryChannel;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordRecoveryService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final AccountStore accounts;
  private final PasswordRecoveryStore recoveries;
  private final PasswordRecoveryMessageSender messages;
  private final PasswordHasher passwords;
  private final int expiresInMinutes;
  private final int resendAfterSeconds;

  public PasswordRecoveryService(
      AccountStore accounts,
      PasswordRecoveryStore recoveries,
      PasswordRecoveryMessageSender messages,
      PasswordHasher passwords,
      @Value("${stowup.recovery.expires-in-minutes:10}") int expiresInMinutes,
      @Value("${stowup.recovery.resend-after-seconds:30}") int resendAfterSeconds) {
    this.accounts = accounts;
    this.recoveries = recoveries;
    this.messages = messages;
    this.passwords = passwords;
    this.expiresInMinutes = expiresInMinutes;
    this.resendAfterSeconds = resendAfterSeconds;
  }

  @Transactional
  public RequestResult request(String identifier, PasswordRecoveryChannel channel) {
    Instant now = Instant.now();
    Optional<Account> found =
        (channel == PasswordRecoveryChannel.EMAIL
                ? accounts.findByEmail(identifier.toLowerCase().trim())
                : accounts.findByPhone(identifier))
            .filter(Account::enabled);
    if (found.isEmpty()) {
      return new RequestResult(
          UUID.randomUUID(),
          genericDestination(channel),
          resendAfterSeconds,
          expiresInMinutes * 60);
    }

    Account account = found.get();
    String destination = destination(account, channel);
    Optional<PasswordRecoveryChallenge> latest = recoveries.findLatest(account.id(), channel);
    if (latest.isPresent() && latest.get().usedAt() == null && !latest.get().canResendAt(now)) {
      int wait =
          Math.max(
              1,
              (int)
                  Math.ceil(
                      Duration.between(now, latest.get().resendAvailableAt()).toMillis() / 1000.0));
      return new RequestResult(
          latest.get().id(), mask(destination, channel), wait, expiresInMinutes * 60);
    }

    recoveries.invalidateActive(account.id(), now);
    String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
    PasswordRecoveryChallenge challenge =
        new PasswordRecoveryChallenge(
            UUID.randomUUID(),
            account.id(),
            channel,
            destination,
            passwords.hash(code),
            null,
            0,
            now.plus(Duration.ofMinutes(expiresInMinutes)),
            now.plus(Duration.ofSeconds(resendAfterSeconds)),
            null,
            null,
            now);
    recoveries.save(challenge);
    messages.send(channel, destination, code, expiresInMinutes);
    return new RequestResult(
        challenge.id(), mask(destination, channel), resendAfterSeconds, expiresInMinutes * 60);
  }

  @Transactional(noRollbackFor = PasswordRecoveryException.class)
  public VerificationResult verify(UUID requestId, String code) {
    Instant now = Instant.now();
    PasswordRecoveryChallenge challenge = requireUsable(requestId, now);
    if (!passwords.matches(code, challenge.codeHash())) {
      recoveries.save(challenge.withFailedAttempt());
      throw new PasswordRecoveryException(
          "Código incorreto. Confira os seis números e tente novamente.");
    }
    String token = randomToken();
    recoveries.save(challenge.verified(passwords.hash(token), now));
    return new VerificationResult(token);
  }

  @Transactional
  public void reset(UUID requestId, String resetToken, String newPassword) {
    Instant now = Instant.now();
    PasswordRecoveryChallenge challenge = requireUsable(requestId, now);
    if (challenge.verifiedAt() == null
        || challenge.resetTokenHash() == null
        || !passwords.matches(resetToken, challenge.resetTokenHash())) {
      throw new PasswordRecoveryException("A validação expirou. Solicite um novo código.");
    }
    Account account =
        accounts
            .findById(challenge.accountId())
            .filter(Account::enabled)
            .orElseThrow(
                () -> new PasswordRecoveryException("Não foi possível redefinir a senha."));
    if (passwords.matches(newPassword, account.passwordHash())) {
      throw new PasswordRecoveryException("A nova senha precisa ser diferente da senha atual.");
    }
    accounts.changePassword(account.id(), passwords.hash(newPassword));
    recoveries.invalidateActive(account.id(), now);
  }

  private PasswordRecoveryChallenge requireUsable(UUID requestId, Instant now) {
    return recoveries
        .findById(requestId)
        .filter(item -> item.isUsableAt(now))
        .orElseThrow(
            () ->
                new PasswordRecoveryException(
                    "Código expirado ou bloqueado. Solicite um novo código."));
  }

  private String destination(Account account, PasswordRecoveryChannel channel) {
    if (channel == PasswordRecoveryChannel.EMAIL) return account.email();
    String digits = account.phone() == null ? "" : account.phone().replaceAll("\\D", "");
    if (digits.length() < 10)
      throw new PasswordRecoveryException("Esta conta não possui um celular válido cadastrado.");
    if (!digits.startsWith("55") && (digits.length() == 10 || digits.length() == 11))
      digits = "55" + digits;
    return "+" + digits;
  }

  private String mask(String destination, PasswordRecoveryChannel channel) {
    if (channel == PasswordRecoveryChannel.SMS)
      return "celular terminado em " + destination.substring(Math.max(0, destination.length() - 4));
    int at = destination.indexOf('@');
    if (at < 1) return "e-mail cadastrado";
    String name = destination.substring(0, at);
    return name.substring(0, Math.min(2, name.length())) + "***" + destination.substring(at);
  }

  private String genericDestination(PasswordRecoveryChannel channel) {
    return channel == PasswordRecoveryChannel.EMAIL ? "e-mail informado" : "celular cadastrado";
  }

  private String randomToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public record RequestResult(
      UUID requestId, String maskedDestination, int resendAfterSeconds, int expiresInSeconds) {}

  public record VerificationResult(String resetToken) {}
}
