package br.com.stowup.staff.application;

import br.com.stowup.staff.application.port.out.StaffGateway;
import br.com.stowup.staff.domain.*;
import java.nio.file.Path;
import java.util.List;

public final class StaffConsoleService {
  private final StaffGateway gateway;

  public StaffConsoleService(StaffGateway gateway) {
    this.gateway = gateway;
  }

  public StaffSession login(String email, String password) {
    if (email == null || email.isBlank() || password == null || password.isBlank()) {
      throw new IllegalArgumentException("Informe usuário e senha.");
    }
    StaffSession session = gateway.login(email.trim(), password);
    if (!session.isMaster()
        && !session.canSupport()
        && !session.canModerate()
        && !session.canReviewDocuments()
        && !session.canReviewContracts()) {
      throw new IllegalArgumentException("Esta conta não possui acesso à central interna.");
    }
    return session;
  }

  public AdminDashboard dashboard(StaffSession session) {
    requireMaster(session);
    return gateway.dashboard(session);
  }

  public List<StaffPayment> payments(StaffSession session) {
    requireMaster(session);
    return gateway.payments(session);
  }

  public byte[] paymentReceipt(StaffSession session, String id) {
    requireMaster(session);
    return gateway.paymentReceipt(session, id);
  }

  public List<AppUser> appUsers(StaffSession session) {
    requireMaster(session);
    return gateway.appUsers(session);
  }

  public AppUser updateAppUser(
      StaffSession session,
      long id,
      String name,
      String phone,
      String city,
      String state,
      String role,
      boolean enabled,
      boolean ownerVerified) {
    requireMaster(session);
    if (name == null || name.trim().length() < 3)
      throw new IllegalArgumentException("Informe o nome completo.");
    if (!"CUSTOMER".equals(role) && !"OWNER".equals(role))
      throw new IllegalArgumentException("Escolha Cliente ou Proprietário.");
    return gateway.updateAppUser(
        session,
        id,
        name.trim(),
        phone == null ? "" : phone.trim(),
        city == null ? "" : city.trim(),
        state == null ? "" : state.trim(),
        role,
        enabled,
        ownerVerified);
  }

  public void deleteAppUser(StaffSession session, long id) {
    requireMaster(session);
    gateway.deleteAppUser(session, id);
  }

  public List<ManagedSpace> managedSpaces(StaffSession session) {
    requireMaster(session);
    return gateway.managedSpaces(session);
  }

  public ManagedSpace updateManagedSpace(StaffSession session, ManagedSpace space) {
    requireMaster(session);
    if (space.title() == null || space.title().trim().length() < 3)
      throw new IllegalArgumentException("Informe o título do anúncio.");
    if (space.description() == null || space.description().trim().length() < 20)
      throw new IllegalArgumentException("A descrição deve ter pelo menos 20 caracteres.");
    if (space.price() == null || space.price().signum() <= 0)
      throw new IllegalArgumentException("Informe um preço válido.");
    return gateway.updateManagedSpace(session, space);
  }

  public ManagedSpace uploadManagedSpacePhoto(StaffSession session, long id, Path file) {
    requireMaster(session);
    return gateway.uploadManagedSpacePhoto(session, id, file);
  }

  public void withdrawManagedSpace(StaffSession session, long id) {
    requireMaster(session);
    gateway.withdrawManagedSpace(session, id);
  }

  public List<AdminSpaceDocument> spaceDocuments(StaffSession session, long spaceId) {
    requireMaster(session);
    return gateway.spaceDocuments(session, spaceId);
  }

  public byte[] downloadSpaceDocument(StaffSession session, String documentId) {
    requireMaster(session);
    return gateway.downloadSpaceDocument(session, documentId);
  }

  public List<SupportTicket> tickets(StaffSession session, String status) {
    requireSupport(session);
    return gateway.tickets(session, status);
  }

  public List<SupportMessage> messages(StaffSession session, String ticketId) {
    requireSupport(session);
    return gateway.messages(session, ticketId);
  }

  public SupportMessage reply(StaffSession session, String ticketId, String body) {
    requireSupport(session);
    if (body == null || body.isBlank()) throw new IllegalArgumentException("Escreva uma resposta.");
    return gateway.reply(session, ticketId, body.trim());
  }

  public SupportMessage uploadSupportAttachment(StaffSession session, String ticketId, Path file) {
    requireSupport(session);
    return gateway.uploadSupportAttachment(session, ticketId, file);
  }

  public byte[] downloadSupportAttachment(StaffSession session, String ticketId, long messageId) {
    requireSupport(session);
    return gateway.downloadSupportAttachment(session, ticketId, messageId);
  }

  public void changeTicketStatus(StaffSession session, String id, String status) {
    requireSupport(session);
    gateway.changeTicketStatus(session, id, status);
  }

  public List<PendingSpace> pendingSpaces(StaffSession session) {
    requireModerator(session);
    return gateway.pendingSpaces(session);
  }

  public void approveSpace(StaffSession session, long id) {
    requireModerator(session);
    gateway.approveSpace(session, id);
  }

  public void rejectSpace(StaffSession session, long id, String reason) {
    requireModerator(session);
    if (reason == null || reason.trim().length() < 10) {
      throw new IllegalArgumentException(
          "Explique o motivo da rejeição com pelo menos 10 caracteres.");
    }
    gateway.rejectSpace(session, id, reason.trim());
  }

  public List<StaffContract> contracts(StaffSession session, String status) {
    requireContracts(session);
    return gateway.contracts(session, status);
  }

  public byte[] signedContract(StaffSession session, String id) {
    requireContracts(session);
    return gateway.signedContract(session, id);
  }

  public void reviewContract(
      StaffSession session,
      String id,
      String result,
      boolean nameMatches,
      String validatorReference,
      String notes) {
    requireContracts(session);
    if (validatorReference == null || validatorReference.isBlank()) {
      throw new IllegalArgumentException(
          "Informe a referência ou resultado emitido pelo VALIDAR/ITI.");
    }
    if (notes == null || notes.trim().length() < 5) {
      throw new IllegalArgumentException("Registre uma observação da análise.");
    }
    gateway.reviewContract(
        session, id, result, nameMatches, validatorReference.trim(), notes.trim());
  }

  public List<StaffUser> staffUsers(StaffSession session) {
    requireMaster(session);
    return gateway.staffUsers(session);
  }

  public StaffUser createStaff(
      StaffSession session, String name, String email, String password, String role) {
    requireMaster(session);
    validateStaff(name, email, password, role);
    return gateway.createStaff(session, name.trim(), email.trim(), password, role);
  }

  public StaffUser updateStaff(
      StaffSession session, long id, String name, String role, boolean enabled) {
    requireMaster(session);
    validateNameAndRole(name, role);
    return gateway.updateStaff(session, id, name.trim(), role, enabled);
  }

  public void resetStaffPassword(StaffSession session, long id, String password) {
    requireMaster(session);
    if (password == null || password.length() < 10) {
      throw new IllegalArgumentException("A senha temporária deve ter pelo menos 10 caracteres.");
    }
    gateway.resetStaffPassword(session, id, password);
  }

  public void deleteStaff(StaffSession session, long id) {
    requireMaster(session);
    gateway.deleteStaff(session, id);
  }

  public List<ListingDocument> listingDocuments(StaffSession session) {
    requireDocuments(session);
    return gateway.listingDocuments(session);
  }

  public byte[] signedListingDocument(StaffSession session, String id) {
    requireDocuments(session);
    return gateway.signedListingDocument(session, id);
  }

  public void reviewListingDocument(
      StaffSession session,
      String id,
      boolean approved,
      boolean nameMatches,
      String reference,
      String notes) {
    requireDocuments(session);
    if (approved && !nameMatches)
      throw new IllegalArgumentException("Confirme que o nome do assinante é o anunciante.");
    if (reference == null || reference.isBlank())
      throw new IllegalArgumentException("Informe a referência do VALIDAR/ITI.");
    if (notes == null || notes.trim().length() < 5)
      throw new IllegalArgumentException("Registre uma observação da análise.");
    gateway.reviewListingDocument(
        session, id, approved, nameMatches, reference.trim(), notes.trim());
  }

  private static void validateStaff(String name, String email, String password, String role) {
    validateNameAndRole(name, role);
    if (email == null || !email.contains("@"))
      throw new IllegalArgumentException("Informe um e-mail corporativo válido.");
    if (password == null || password.length() < 10)
      throw new IllegalArgumentException("A senha temporária deve ter pelo menos 10 caracteres.");
  }

  private static void validateNameAndRole(String name, String role) {
    if (name == null || name.trim().length() < 3)
      throw new IllegalArgumentException("Informe o nome completo.");
    if (!"SUPPORT".equals(role)
        && !"LISTING_ANALYST".equals(role)
        && !"DOCUMENT_ANALYST".equals(role)
        && !"CONTRACT_ANALYST".equals(role))
      throw new IllegalArgumentException("Escolha a função do funcionário.");
  }

  private static void requireSupport(StaffSession session) {
    if (!session.canSupport())
      throw new IllegalArgumentException("Esta conta não possui acesso ao atendimento.");
  }

  private static void requireModerator(StaffSession session) {
    if (!session.canModerate())
      throw new IllegalArgumentException("Esta conta não possui acesso à análise e moderação.");
  }

  private static void requireDocuments(StaffSession session) {
    if (!session.canReviewDocuments())
      throw new IllegalArgumentException(
          "Esta conta não possui acesso à documentação do anunciante.");
  }

  private static void requireContracts(StaffSession session) {
    if (!session.canReviewContracts())
      throw new IllegalArgumentException("Esta conta não possui acesso aos contratos de locação.");
  }

  private static void requireMaster(StaffSession session) {
    if (!session.isMaster())
      throw new IllegalArgumentException("Apenas o usuário mestre pode gerenciar a equipe.");
  }
}
