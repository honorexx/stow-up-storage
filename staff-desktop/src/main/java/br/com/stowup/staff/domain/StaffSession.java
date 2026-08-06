package br.com.stowup.staff.domain;

public record StaffSession(String token, String name, String email, String role) {
  public boolean isMaster() {
    return "ADMIN".equals(role);
  }

  public boolean canSupport() {
    return isMaster() || "SUPPORT".equals(role);
  }

  public boolean canModerate() {
    return isMaster() || "MODERATOR".equals(role) || "LISTING_ANALYST".equals(role);
  }

  public boolean canReviewDocuments() {
    return isMaster() || "DOCUMENT_ANALYST".equals(role);
  }

  public boolean canReviewContracts() {
    return isMaster() || "MODERATOR".equals(role) || "CONTRACT_ANALYST".equals(role);
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
}
