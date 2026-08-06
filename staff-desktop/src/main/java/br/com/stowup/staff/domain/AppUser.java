package br.com.stowup.staff.domain;

import java.time.Instant;

public record AppUser(
    long id,
    String name,
    String email,
    String phone,
    String city,
    String state,
    String role,
    boolean enabled,
    boolean ownerVerified,
    String photoUrl,
    long listings,
    Instant createdAt) {
  public String roleLabel() {
    return "OWNER".equals(role) ? "Proprietário" : "Cliente";
  }

  @Override
  public String toString() {
    return name
        + " · "
        + roleLabel()
        + "\n"
        + email
        + " · "
        + (enabled ? "Ativo" : "Bloqueado")
        + " · "
        + listings
        + " anúncio(s)";
  }
}
