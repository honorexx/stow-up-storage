package br.com.stowup.user;

import br.com.stowup.account.application.AccountService;
import br.com.stowup.user.AuthController.UserView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
  private final AccountService accounts;

  public ProfileController(AccountService accounts) {
    this.accounts = accounts;
  }

  @GetMapping
  public UserView get(Principal principal) {
    return UserView.from(accounts.profile(principal.getName()));
  }

  @PatchMapping
  public UserView update(@Valid @RequestBody UpdateProfileRequest request, Principal principal) {
    return UserView.from(
        accounts.updateProfile(
            principal.getName(),
            new AccountService.ProfileUpdate(
                request.name(),
                request.phone(),
                request.postalCode(),
                request.addressLine(),
                request.addressNumber(),
                request.addressComplement(),
                request.district(),
                request.city(),
                request.state())));
  }

  @DeleteMapping
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void close(@Valid @RequestBody DeleteAccountRequest request, Principal principal) {
    accounts.closeAccount(principal.getName(), request.password());
  }

  public record UpdateProfileRequest(
      @NotBlank @Size(min = 3, max = 120) String name,
      @NotBlank @Pattern(regexp = "^[+0-9 ()-]{8,24}$", message = "Telefone inválido") String phone,
      @NotBlank @Pattern(regexp = "^[0-9-]{8,9}$") String postalCode,
      @NotBlank @Size(max = 180) String addressLine,
      @NotBlank @Size(max = 20) String addressNumber,
      @Size(max = 80) String addressComplement,
      @NotBlank @Size(max = 80) String district,
      @NotBlank @Size(max = 80) String city,
      @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String state) {}

  public record DeleteAccountRequest(@NotBlank @Size(min = 8, max = 200) String password) {}
}
