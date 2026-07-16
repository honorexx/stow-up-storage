package br.com.stowup.account.adapter.out.persistence;

import br.com.stowup.account.domain.PasswordRecoveryChannel;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PasswordRecoveryJpaRepository extends JpaRepository<PasswordRecoveryEntity, UUID> {
  Optional<PasswordRecoveryEntity> findTopByAccountIdAndChannelOrderByCreatedAtDesc(
      Long accountId, PasswordRecoveryChannel channel);

  @Modifying
  @Query(
      "update PasswordRecoveryEntity r set r.usedAt = :at where r.accountId = :accountId and r.usedAt is null")
  void invalidateActive(@Param("accountId") Long accountId, @Param("at") Instant at);
}
