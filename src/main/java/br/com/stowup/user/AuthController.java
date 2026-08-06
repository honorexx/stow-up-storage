package br.com.stowup.user;

import br.com.stowup.account.application.AccountService;
import br.com.stowup.account.application.InvalidCredentialsException;
import br.com.stowup.account.application.PasswordRecoveryService;
import br.com.stowup.account.domain.Account;
import br.com.stowup.account.domain.AccountRole;
import br.com.stowup.account.domain.PasswordRecoveryChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AccountService accounts;
  private final PasswordRecoveryService passwordRecovery;

  public AuthController(AccountService accounts, PasswordRecoveryService passwordRecovery) {
    this.accounts = accounts;
    this.passwordRecovery = passwordRecovery;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return AuthResponse.from(
        accounts.register(
            new AccountService.Registration(
                request.name(),
                request.email(),
                request.password(),
                request.phone(),
                request.cpf(),
                request.postalCode(),
                request.addressLine(),
                request.addressNumber(),
                request.addressComplement(),
                request.district(),
                request.city(),
                request.state(),
                request.acceptedTerms(),
                request.termsVersion())));
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return AuthResponse.from(accounts.authenticate(request.email(), request.password()));
  }

  @PostMapping("/staff-login")
  public AuthResponse staffLogin(@Valid @RequestBody LoginRequest request) {
    AccountService.Session session = accounts.authenticate(request.email(), request.password());
    if (isPublicRole(session.account().role())) throw new InvalidCredentialsException();
    return AuthResponse.from(session);
  }

  private static boolean isPublicRole(AccountRole role) {
    return role == AccountRole.CUSTOMER || role == AccountRole.OWNER;
  }

  @PostMapping("/password-recovery/request")
  public PasswordRecoveryService.RequestResult requestPasswordRecovery(
      @Valid @RequestBody PasswordRecoveryRequest request) {
    return passwordRecovery.request(request.identifier(), request.channel());
  }

  @PostMapping("/password-recovery/verify")
  public PasswordRecoveryService.VerificationResult verifyPasswordRecovery(
      @Valid @RequestBody PasswordRecoveryVerification request) {
    return passwordRecovery.verify(request.requestId(), request.code());
  }

  @PostMapping("/password-recovery/reset")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordReset request) {
    passwordRecovery.reset(request.requestId(), request.resetToken(), request.newPassword());
    return ResponseEntity.noContent().build();
  }

  public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

  public record PasswordRecoveryRequest(
      @NotBlank @Size(max = 180) String identifier, @NotNull PasswordRecoveryChannel channel) {}

  public record PasswordRecoveryVerification(
      @NotNull UUID requestId, @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code) {}

  public record PasswordReset(
      @NotNull UUID requestId,
      @NotBlank String resetToken,
      @NotBlank @Size(min = 8, max = 72) String newPassword) {}

  public record RegisterRequest(
      @NotBlank @Size(min = 3, max = 120) String name,
      @NotBlank @Email String email,
      @NotBlank @Size(min = 8, max = 72) String password,
      @NotBlank @Pattern(regexp = "^[+0-9 ()-]{8,24}$") String phone,
      @NotBlank String cpf,
      @NotBlank @Pattern(regexp = "^[0-9-]{8,9}$") String postalCode,
      @NotBlank @Size(max = 180) String addressLine,
      @NotBlank @Size(max = 20) String addressNumber,
      @Size(max = 80) String addressComplement,
      @NotBlank @Size(max = 80) String district,
      @NotBlank @Size(max = 80) String city,
      @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String state,
      boolean acceptedTerms,
      @NotBlank String termsVersion) {}

  public record AuthResponse(String token, UserView user) {
    static AuthResponse from(AccountService.Session session) {
      return new AuthResponse(session.token(), UserView.from(session.account()));
    }
  }

  public record UserView(
      Long id,
      String name,
      String email,
      String phone,
      String cpfMasked,
      String postalCode,
      String addressLine,
      String addressNumber,
      String addressComplement,
      String district,
      String city,
      String state,
      String photoUrl,
      AccountRole role,
      Set<AccountRole> additionalRoles) {
    public static UserView from(Account account) {
      return new UserView(
          account.id(),
          account.name(),
          account.email(),
          account.phone(),
          account.maskedCpf(),
          account.postalCode(),
          account.addressLine(),
          account.addressNumber(),
          account.addressComplement(),
          account.district(),
          account.city(),
          account.state(),
          account.photoUrl(),
          account.role(),
          account.additionalRoles() == null ? Set.of() : account.additionalRoles());
    }
  }
}
