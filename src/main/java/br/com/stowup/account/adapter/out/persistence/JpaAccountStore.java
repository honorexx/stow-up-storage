package br.com.stowup.account.adapter.out.persistence;

import br.com.stowup.account.application.port.out.AccountStore;
import br.com.stowup.account.application.port.out.SensitiveDataProtector;
import br.com.stowup.account.domain.Account;
import br.com.stowup.account.domain.AccountRole;
import br.com.stowup.user.Role;
import br.com.stowup.user.UserAccount;
import br.com.stowup.user.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaAccountStore implements AccountStore {
  private final UserRepository repository;
  private final SensitiveDataProtector protector;

  public JpaAccountStore(UserRepository repository, SensitiveDataProtector protector) {
    this.repository = repository;
    this.protector = protector;
  }

  @Override
  public Optional<Account> findByEmail(String email) {
    return repository.findByEmailIgnoreCase(email).map(this::toDomain);
  }

  @Override
  public Optional<Account> findByLogin(String login) {
    String value = login == null ? "" : login.trim();
    return (value.toUpperCase().matches("SU\\d{6}")
            ? repository.findByStaffCodeIgnoreCase(value)
            : repository.findByEmailIgnoreCase(value))
        .map(this::toDomain);
  }

  @Override
  public Optional<Account> findById(Long id) {
    return repository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<Account> findByPhone(String phone) {
    return repository.findByPhoneNormalized(normalizePhone(phone)).map(this::toDomain);
  }

  @Override
  public boolean existsByEmail(String email) {
    return repository.existsByEmailIgnoreCase(email);
  }

  @Override
  public boolean existsByCpf(String cpf) {
    return repository.existsByCpfHash(protector.fingerprint(cpf));
  }

  @Override
  public boolean existsByPhone(String phone) {
    return repository.existsByPhoneNormalized(normalizePhone(phone));
  }

  @Override
  public Account save(Account account) {
    UserAccount entity;
    if (account.id() == null) {
      entity =
          new UserAccount(
              account.name(),
              account.email(),
              account.passwordHash(),
              Role.valueOf(account.role().name()));
    } else {
      entity = repository.findById(account.id()).orElseThrow();
      entity.setName(account.name());
    }
    entity.setPhone(account.phone());
    if (account.cpf() != null) {
      entity.setCpfEncrypted(protector.protect(account.cpf()));
      entity.setCpfHash(protector.fingerprint(account.cpf()));
    }
    entity.setPostalCode(account.postalCode());
    entity.setAddressLine(account.addressLine());
    entity.setAddressNumber(account.addressNumber());
    entity.setAddressComplement(account.addressComplement());
    entity.setDistrict(account.district());
    entity.setCity(account.city());
    entity.setState(account.state());
    entity.setPhotoUrl(account.photoUrl());
    entity.setRole(Role.valueOf(account.role().name()));
    return toDomain(repository.save(entity));
  }

  @Override
  public void changePassword(Long accountId, String passwordHash) {
    UserAccount entity = repository.findById(accountId).orElseThrow();
    entity.setPasswordHash(passwordHash);
    repository.save(entity);
  }

  @Override
  public void close(Long accountId, String anonymizedEmail, String unusablePasswordHash) {
    UserAccount entity = repository.findById(accountId).orElseThrow();
    entity.close(anonymizedEmail, unusablePasswordHash);
    repository.save(entity);
  }

  private Account toDomain(UserAccount entity) {
    return new Account(
        entity.getId(),
        entity.getName(),
        entity.getEmail(),
        entity.getPasswordHash(),
        entity.getPhone(),
        protector.reveal(entity.getCpfEncrypted()),
        entity.getPostalCode(),
        entity.getAddressLine(),
        entity.getAddressNumber(),
        entity.getAddressComplement(),
        entity.getDistrict(),
        entity.getCity(),
        entity.getState(),
        entity.getPhotoUrl(),
        AccountRole.valueOf(entity.getRole().name()),
        entity.isEnabled(),
        entity.getCreatedAt());
  }

  private String normalizePhone(String value) {
    String digits = value == null ? "" : value.replaceAll("\\D", "");
    return digits.length() == 10 || digits.length() == 11 ? "55" + digits : digits;
  }
}
