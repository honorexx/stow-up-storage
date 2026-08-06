package br.com.stowup.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.stowup.account.adapter.out.security.BCryptPasswordHasher;
import br.com.stowup.account.application.PasswordRecoveryException;
import br.com.stowup.account.application.PasswordRecoveryService;
import br.com.stowup.account.application.port.out.AccountStore;
import br.com.stowup.account.application.port.out.PasswordRecoveryMessageSender;
import br.com.stowup.account.application.port.out.PasswordRecoveryStore;
import br.com.stowup.account.domain.Account;
import br.com.stowup.account.domain.AccountRole;
import br.com.stowup.account.domain.PasswordRecoveryChallenge;
import br.com.stowup.account.domain.PasswordRecoveryChannel;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordRecoveryServiceTest {
  private final AccountStore accounts = mock(AccountStore.class);
  private final PasswordRecoveryStore recoveries = mock(PasswordRecoveryStore.class);
  private final PasswordRecoveryMessageSender messages = mock(PasswordRecoveryMessageSender.class);
  private final BCryptPasswordHasher passwords =
      new BCryptPasswordHasher(new BCryptPasswordEncoder());
  private final AtomicReference<PasswordRecoveryChallenge> saved = new AtomicReference<>();
  private final AtomicReference<String> deliveredCode = new AtomicReference<>();
  private PasswordRecoveryService service;
  private Account account;

  @BeforeEach
  void setUp() {
    service = new PasswordRecoveryService(accounts, recoveries, messages, passwords, 10, 30);
    account =
        new Account(
            10L,
            "Cliente StowUp",
            "cliente@stowup.com.br",
            passwords.hash("SenhaAntiga123"),
            "+5541999999999",
            "12345678909",
            "80000000",
            "Rua Teste",
            "10",
            null,
            "Centro",
            "Curitiba",
            "PR",
            null,
            AccountRole.CUSTOMER,
            java.util.Set.of(),
            true,
            Instant.now());
    when(accounts.findByEmail(account.email())).thenReturn(Optional.of(account));
    when(accounts.findById(account.id())).thenReturn(Optional.of(account));
    when(recoveries.findLatest(account.id(), PasswordRecoveryChannel.EMAIL))
        .thenReturn(Optional.empty());
    when(recoveries.save(any()))
        .thenAnswer(
            invocation -> {
              PasswordRecoveryChallenge challenge = invocation.getArgument(0);
              saved.set(challenge);
              return challenge;
            });
    when(recoveries.findById(any())).thenAnswer(invocation -> Optional.ofNullable(saved.get()));
    doAnswer(
            invocation -> {
              deliveredCode.set(invocation.getArgument(2));
              return null;
            })
        .when(messages)
        .send(eq(PasswordRecoveryChannel.EMAIL), eq(account.email()), any(), eq(10));
  }

  @Test
  void recoversPasswordOnlyAfterCorrectCode() {
    PasswordRecoveryService.RequestResult requested =
        service.request(account.email(), PasswordRecoveryChannel.EMAIL);

    assertEquals(30, requested.resendAfterSeconds());
    assertEquals(6, deliveredCode.get().length());

    assertThrows(
        PasswordRecoveryException.class, () -> service.verify(requested.requestId(), "00000X"));
    assertEquals(1, saved.get().failedAttempts());

    PasswordRecoveryService.VerificationResult verified =
        service.verify(requested.requestId(), deliveredCode.get());
    service.reset(requested.requestId(), verified.resetToken(), "SenhaNova456");

    verify(accounts)
        .changePassword(eq(account.id()), argThat(hash -> passwords.matches("SenhaNova456", hash)));
    verify(recoveries, times(2)).invalidateActive(eq(account.id()), any());
  }
}
