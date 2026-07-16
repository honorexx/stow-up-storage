package br.com.stowup.moderation.application;

import br.com.stowup.audit.AuditService;
import br.com.stowup.listingdocument.application.ListingDocumentService;
import br.com.stowup.notification.NotificationService;
import br.com.stowup.space.Space;
import br.com.stowup.space.SpaceRepository;
import br.com.stowup.user.UserAccount;
import br.com.stowup.user.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SpaceModerationService {
  private final SpaceRepository spaces;
  private final UserRepository users;
  private final ListingDocumentService listingDocuments;
  private final NotificationService notifications;
  private final AuditService audit;
  private final int minimumPhotos;

  public SpaceModerationService(
      SpaceRepository spaces,
      UserRepository users,
      ListingDocumentService listingDocuments,
      NotificationService notifications,
      AuditService audit,
      @Value("${stowup.listings.minimum-photos:4}") int minimumPhotos) {
    this.spaces = spaces;
    this.users = users;
    this.listingDocuments = listingDocuments;
    this.notifications = notifications;
    this.audit = audit;
    this.minimumPhotos = minimumPhotos;
  }

  @Transactional(readOnly = true)
  public List<Space> pending() {
    return spaces.findByModerationStatusOrderByIdDesc("PENDING");
  }

  @Transactional
  public Space approve(long spaceId, String moderatorEmail) {
    Space space = requireSpace(spaceId);
    if (!listingDocuments.isApproved(spaceId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A documentação assinada do anunciante ainda não foi aprovada.");
    }
    if (space.getPhotoUrls().size() < minimumPhotos) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O anúncio precisa ter pelo menos " + minimumPhotos + " fotos antes da aprovação.");
    }
    space.approve(requireStaff(moderatorEmail));
    notifications.send(
        space.getOwner(),
        "SPACE_APPROVED",
        "Anúncio aprovado",
        space.getTitle() + " já está ativo na busca.",
        "SPACE",
        spaceId);
    audit.record(moderatorEmail, "SPACE_APPROVED", "SPACE", spaceId, null);
    return space;
  }

  @Transactional
  public Space reject(long spaceId, String moderatorEmail, String reason) {
    Space space = requireSpace(spaceId);
    space.reject(requireStaff(moderatorEmail), reason.trim());
    notifications.send(
        space.getOwner(),
        "SPACE_REJECTED",
        "Anúncio devolvido para correção",
        reason.trim(),
        "SPACE",
        spaceId);
    audit.record(moderatorEmail, "SPACE_REJECTED", "SPACE", spaceId, reason.trim());
    return space;
  }

  @Transactional
  public Space suspend(long spaceId, String moderatorEmail, String reason) {
    Space space = requireSpace(spaceId);
    space.suspendByAdmin(reason.trim());
    notifications.send(
        space.getOwner(), "SPACE_SUSPENDED", "Anúncio suspenso", reason.trim(), "SPACE", spaceId);
    audit.record(moderatorEmail, "SPACE_SUSPENDED", "SPACE", spaceId, reason.trim());
    return space;
  }

  private Space requireSpace(long id) {
    return spaces
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anúncio não encontrado."));
  }

  private UserAccount requireStaff(String email) {
    return users
        .findByEmailIgnoreCase(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }
}
