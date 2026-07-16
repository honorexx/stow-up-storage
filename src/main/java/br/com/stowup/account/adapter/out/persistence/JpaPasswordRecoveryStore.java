package br.com.stowup.account.adapter.out.persistence;

import br.com.stowup.account.application.port.out.PasswordRecoveryStore;
import br.com.stowup.account.domain.PasswordRecoveryChallenge;
import br.com.stowup.account.domain.PasswordRecoveryChannel;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaPasswordRecoveryStore implements PasswordRecoveryStore {
  private final PasswordRecoveryJpaRepository repository;

  public JpaPasswordRecoveryStore(PasswordRecoveryJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<PasswordRecoveryChallenge> findById(UUID id) {
    return repository.findById(id).map(PasswordRecoveryEntity::toDomain);
  }

  @Override
  public Optional<PasswordRecoveryChallenge> findLatest(
      Long accountId, PasswordRecoveryChannel channel) {
    return repository
        .findTopByAccountIdAndChannelOrderByCreatedAtDesc(accountId, channel)
        .map(PasswordRecoveryEntity::toDomain);
  }

  @Override
  public PasswordRecoveryChallenge save(PasswordRecoveryChallenge challenge) {
    return repository.save(PasswordRecoveryEntity.from(challenge)).toDomain();
  }

  @Override
  public void invalidateActive(Long accountId, Instant at) {
    repository.invalidateActive(accountId, at);
  }
}
