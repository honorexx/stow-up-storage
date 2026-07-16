package br.com.stowup.account.application.port.out;

import br.com.stowup.account.domain.Account;
import java.util.Optional;

public interface AccountStore {
  Optional<Account> findByEmail(String email);

  Optional<Account> findByLogin(String login);

  Optional<Account> findById(Long id);

  Optional<Account> findByPhone(String phone);

  boolean existsByEmail(String email);

  boolean existsByCpf(String cpf);

  boolean existsByPhone(String phone);

  Account save(Account account);

  void changePassword(Long accountId, String passwordHash);

  void close(Long accountId, String anonymizedEmail, String unusablePasswordHash);
}
