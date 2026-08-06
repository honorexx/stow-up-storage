package br.com.stowup.staff.application.port.out;

import br.com.stowup.staff.domain.*;
import java.nio.file.Path;
import java.util.List;

public interface StaffGateway {
  StaffSession login(String email, String password);

  AdminDashboard dashboard(StaffSession session);

  List<StaffPayment> payments(StaffSession session);

  byte[] paymentReceipt(StaffSession session, String id);

  List<AppUser> appUsers(StaffSession session);

  AppUser updateAppUser(
      StaffSession session,
      long id,
      String name,
      String phone,
      String city,
      String state,
      String role,
      boolean enabled,
      boolean ownerVerified);

  void deleteAppUser(StaffSession session, long id);

  List<ManagedSpace> managedSpaces(StaffSession session);

  ManagedSpace updateManagedSpace(StaffSession session, ManagedSpace space);

  ManagedSpace uploadManagedSpacePhoto(StaffSession session, long id, Path file);

  void withdrawManagedSpace(StaffSession session, long id);

  List<AdminSpaceDocument> spaceDocuments(StaffSession session, long spaceId);

  byte[] downloadSpaceDocument(StaffSession session, String documentId);

  List<SupportTicket> tickets(StaffSession session, String status);

  List<SupportMessage> messages(StaffSession session, String ticketId);

  SupportMessage reply(StaffSession session, String ticketId, String body);

  SupportMessage uploadSupportAttachment(StaffSession session, String ticketId, Path file);

  byte[] downloadSupportAttachment(StaffSession session, String ticketId, long messageId);

  void changeTicketStatus(StaffSession session, String ticketId, String status);

  List<PendingSpace> pendingSpaces(StaffSession session);

  void approveSpace(StaffSession session, long spaceId);

  void rejectSpace(StaffSession session, long spaceId, String reason);

  List<StaffContract> contracts(StaffSession session, String status);

  byte[] signedContract(StaffSession session, String contractId);

  void reviewContract(
      StaffSession session,
      String contractId,
      String result,
      boolean signerNameMatches,
      String validatorReference,
      String notes);

  List<StaffUser> staffUsers(StaffSession session);

  StaffUser createStaff(
      StaffSession session, String name, String email, String password, String role);

  StaffUser updateStaff(StaffSession session, long id, String name, String role, boolean enabled);

  void resetStaffPassword(StaffSession session, long id, String password);

  void deleteStaff(StaffSession session, long id);

  List<ListingDocument> listingDocuments(StaffSession session);

  byte[] signedListingDocument(StaffSession session, String id);

  void reviewListingDocument(
      StaffSession session,
      String id,
      boolean approved,
      boolean signerNameMatches,
      String validatorReference,
      String notes);
}
