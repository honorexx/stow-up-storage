package br.com.stowup.account.application.port.out;

import br.com.stowup.account.domain.PasswordRecoveryChallenge;
import br.com.stowup.account.domain.PasswordRecoveryChannel;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordRecoveryStore {
  Optional<PasswordRecoveryChallenge> findById(UUID id);

  Optional<PasswordRecoveryChallenge> findLatest(Long accountId, PasswordRecoveryChannel channel);

  PasswordRecoveryChallenge save(PasswordRecoveryChallenge challenge);

  void invalidateActive(Long accountId, Instant at);
}
