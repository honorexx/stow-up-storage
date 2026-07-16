package br.com.stowup.staff.domain;

public record StaffUser(
    long id, String staffCode, String name, String email, String role, boolean enabled) {
  public boolean isMaster() {
    return "ADMIN".equals(role);
  }

  public String roleLabel() {
    return switch (role) {
      case "ADMIN" -> "Acesso mestre";
      case "MODERATOR" -> "Análise e moderação";
      case "LISTING_ANALYST" -> "Análise de anúncios";
      case "DOCUMENT_ANALYST" -> "Documentação do anunciante";
      case "CONTRACT_ANALYST" -> "Contratos de locação";
      case "SUPPORT" -> "Atendimento e suporte";
      default -> role;
    };
  }

  @Override
  public String toString() {
    return (staffCode == null ? "" : staffCode + " · ")
        + name
        + "\n"
        + roleLabel()
        + " · "
        + (enabled ? "Ativo" : "Bloqueado");
  }
}
