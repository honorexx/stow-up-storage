package br.com.stowup.account.application;

import br.com.stowup.account.application.port.out.AccessTokenIssuer;
import br.com.stowup.account.application.port.out.AccountStore;
import br.com.stowup.account.application.port.out.OwnedListingWithdrawal;
import br.com.stowup.account.application.port.out.PasswordHasher;
import br.com.stowup.account.application.port.out.TermsAcceptanceRecorder;
import br.com.stowup.account.domain.Account;
import br.com.stowup.account.domain.AccountRole;
import br.com.stowup.account.domain.Cpf;
import br.com.stowup.legal.TermsPolicy;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private final AccountStore accounts;
  private final PasswordHasher passwords;
  private final AccessTokenIssuer tokens;
  private final TermsAcceptanceRecorder termsAcceptances;
  private final OwnedListingWithdrawal listings;

  public AccountService(
      AccountStore accounts,
      PasswordHasher passwords,
      AccessTokenIssuer tokens,
      TermsAcceptanceRecorder termsAcceptances,
      OwnedListingWithdrawal listings) {
    this.accounts = accounts;
    this.passwords = passwords;
    this.tokens = tokens;
    this.termsAcceptances = termsAcceptances;
    this.listings = listings;
  }

  @Transactional
  public Session register(Registration command) {
    if (!command.acceptedTerms() || !TermsPolicy.CURRENT_VERSION.equals(command.termsVersion()))
      throw new InvalidAccountDataException(
          "Leia e aceite a versão atual dos Termos de Uso para criar a conta.");
    String email = command.email().toLowerCase().trim();
    if (accounts.existsByEmail(email))
      throw new AccountConflictException("Este e-mail já está cadastrado.");
    if (accounts.existsByPhone(command.phone()))
      throw new AccountConflictException("Este celular já está cadastrado.");
    final String cpf;
    try {
      cpf = Cpf.normalize(command.cpf());
    } catch (IllegalArgumentException exception) {
      throw new InvalidAccountDataException(exception.getMessage());
    }
    if (accounts.existsByCpf(cpf))
      throw new AccountConflictException("Este CPF já está cadastrado.");
    Account account =
        accounts.save(
            new Account(
                null,
                command.name().trim(),
                email,
                passwords.hash(command.password()),
                command.phone().trim(),
                cpf,
                digits(command.postalCode()),
                command.addressLine().trim(),
                command.addressNumber().trim(),
                optional(command.addressComplement()),
                command.district().trim(),
                command.city().trim(),
                command.state().trim().toUpperCase(),
                null,
                AccountRole.CUSTOMER,
                Set.of(),
                true,
                Instant.now()));
    termsAcceptances.record(account.id(), TermsPolicy.CODE, TermsPolicy.CURRENT_VERSION);
    return new Session(tokens.issue(account), account);
  }

  @Transactional(readOnly = true)
  public Session authenticate(String login, String password) {
    Account account =
        accounts
            .findByLogin(login)
            .filter(Account::enabled)
            .orElseThrow(InvalidCredentialsException::new);
    if (!passwords.matches(password, account.passwordHash()))
      throw new InvalidCredentialsException();
    return new Session(tokens.issue(account), account);
  }

  @Transactional(readOnly = true)
  public Account profile(String email) {
    return accounts.findByEmail(email).orElseThrow(AccountNotFoundException::new);
  }

  @Transactional
  public Account updateProfile(String email, ProfileUpdate command) {
    Account current = profile(email);
    accounts
        .findByPhone(command.phone())
        .filter(found -> !found.id().equals(current.id()))
        .ifPresent(
            found -> {
              throw new AccountConflictException("Este celular já está cadastrado em outra conta.");
            });
    return accounts.save(
        current.withProfile(
            command.name().trim(),
            command.phone().trim(),
            digits(command.postalCode()),
            command.addressLine().trim(),
            command.addressNumber().trim(),
            optional(command.addressComplement()),
            command.district().trim(),
            command.city().trim(),
            command.state().trim().toUpperCase()));
  }

  @Transactional
  public Account updatePhoto(String email, String photoUrl) {
    return accounts.save(profile(email).withPhoto(photoUrl));
  }

  @Transactional
  public Account promoteToOwner(String email) {
    Account current = profile(email);
    return current.role() == AccountRole.CUSTOMER
        ? accounts.save(current.withRole(AccountRole.OWNER))
        : current;
  }

  @Transactional(readOnly = true)
  public void requirePassword(String email, String password) {
    Account current = profile(email);
    if (!passwords.matches(password, current.passwordHash()))
      throw new InvalidCredentialsException();
  }

  @Transactional
  public void closeAccount(String email, String password) {
    Account current = profile(email);
    if (current.role() != AccountRole.CUSTOMER && current.role() != AccountRole.OWNER)
      throw new InvalidAccountDataException(
          "Contas internas devem ser desativadas pelo usuário mestre.");
    if (!passwords.matches(password, current.passwordHash()))
      throw new InvalidCredentialsException();
    listings.withdrawAllByOwner(current.id());
    String anonymizedEmail =
        "deleted-" + current.id() + "-" + UUID.randomUUID() + "@deleted.stowup";
    accounts.close(current.id(), anonymizedEmail, passwords.hash(UUID.randomUUID().toString()));
  }

  private static String digits(String value) {
    return value == null ? "" : value.replaceAll("\\D", "");
  }

  private static String optional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public record Registration(
      String name,
      String email,
      String password,
      String phone,
      String cpf,
      String postalCode,
      String addressLine,
      String addressNumber,
      String addressComplement,
      String district,
      String city,
      String state,
      boolean acceptedTerms,
      String termsVersion) {}

  public record ProfileUpdate(
      String name,
      String phone,
      String postalCode,
      String addressLine,
      String addressNumber,
      String addressComplement,
      String district,
      String city,
      String state) {}

  public record Session(String token, Account account) {}
}
