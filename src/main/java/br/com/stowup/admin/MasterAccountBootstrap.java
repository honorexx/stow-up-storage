package br.com.stowup.admin;

import br.com.stowup.user.Role;
import br.com.stowup.user.UserAccount;
import br.com.stowup.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MasterAccountBootstrap implements ApplicationRunner {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final boolean enabled;
  private final String name;
  private final String email;
  private final String password;

  public MasterAccountBootstrap(
      UserRepository users,
      PasswordEncoder passwords,
      @Value("${stowup.master.enabled:false}") boolean enabled,
      @Value("${stowup.master.name:Usuário Mestre}") String name,
      @Value("${stowup.master.email:master@stowup.local}") String email,
      @Value("${stowup.master.password:}") String password) {
    this.users = users;
    this.passwords = passwords;
    this.enabled = enabled;
    this.name = repairLegacyEncoding(name);
    this.email = email;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!enabled) return;
    if (password == null || password.length() < 12)
      throw new IllegalStateException(
          "STOWUP_MASTER_PASSWORD deve possuir pelo menos 12 caracteres.");

    users
        .findByEmailIgnoreCase(email)
        .ifPresentOrElse(
            existing -> {
              if (looksLikeLegacyMojibake(existing.getName())) {
                existing.setName(this.name.trim());
              }
              existing.setRole(Role.ADMIN);
              existing.setEnabled(true);
              if (existing.getStaffCode() == null) existing.setStaffCode(nextCode());
            },
            () -> {
              UserAccount master =
                  new UserAccount(
                      name.trim(), email.trim(), passwords.encode(password), Role.ADMIN);
              master.setStaffCode(nextCode());
              users.save(master);
            });
  }

  private String nextCode() {
    for (int i = 0; i < 100; i++) {
      String code = "SU%06d".formatted(RANDOM.nextInt(1_000_000));
      if (!users.existsByStaffCodeIgnoreCase(code)) return code;
    }
    throw new IllegalStateException("Não foi possível gerar o código da conta mestre.");
  }

  static String repairLegacyEncoding(String value) {
    if (!looksLikeLegacyMojibake(value)) return value;
    return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
  }

  private static boolean looksLikeLegacyMojibake(String value) {
    if (value == null) return false;
    for (int i = 0; i + 1 < value.length(); i++) {
      char first = value.charAt(i);
      char second = value.charAt(i + 1);
      if ((first == 'Ã' || first == 'Â') && second >= '\u0080' && second <= '\u00bf') {
        return true;
      }
    }
    return false;
  }
}
