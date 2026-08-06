package br.com.stowup.account.domain;

import java.time.Instant;
import java.util.Set;

public record Account(
    Long id,
    String name,
    String email,
    String passwordHash,
    String phone,
    String cpf,
    String postalCode,
    String addressLine,
    String addressNumber,
    String addressComplement,
    String district,
    String city,
    String state,
    String photoUrl,
    AccountRole role,
    Set<AccountRole> additionalRoles,
    boolean enabled,
    Instant createdAt) {
  public Account withProfile(
      String name,
      String phone,
      String postalCode,
      String addressLine,
      String addressNumber,
      String addressComplement,
      String district,
      String city,
      String state) {
    return new Account(
        id,
        name,
        email,
        passwordHash,
        phone,
        cpf,
        postalCode,
        addressLine,
        addressNumber,
        addressComplement,
        district,
        city,
        state,
        photoUrl,
        role,
        additionalRoles,
        enabled,
        createdAt);
  }

  public Account withPhoto(String photoUrl) {
    return new Account(
        id,
        name,
        email,
        passwordHash,
        phone,
        cpf,
        postalCode,
        addressLine,
        addressNumber,
        addressComplement,
        district,
        city,
        state,
        photoUrl,
        role,
        additionalRoles,
        enabled,
        createdAt);
  }

  public Account withRole(AccountRole role) {
    return new Account(
        id,
        name,
        email,
        passwordHash,
        phone,
        cpf,
        postalCode,
        addressLine,
        addressNumber,
        addressComplement,
        district,
        city,
        state,
        photoUrl,
        role,
        additionalRoles,
        enabled,
        createdAt);
  }

  public String maskedCpf() {
    return cpf == null || cpf.length() != 11 ? null : "***.***.***-" + cpf.substring(9);
  }
}
