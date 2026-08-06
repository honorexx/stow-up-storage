package br.com.stowup.listingdocument.application;

import br.com.stowup.audit.AuditService;
import br.com.stowup.listingdocument.adapter.out.persistence.OwnerListingDocumentRepository;
import br.com.stowup.listingdocument.application.port.out.ListingDocumentGenerator;
import br.com.stowup.listingdocument.domain.ListingDocumentStatus;
import br.com.stowup.listingdocument.domain.OwnerListingDocument;
import br.com.stowup.notification.DocumentEmailService;
import br.com.stowup.notification.NotificationService;
import br.com.stowup.space.Space;
import br.com.stowup.user.UserAccount;
import br.com.stowup.user.UserRepository;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ListingDocumentService {
  private final OwnerListingDocumentRepository documents;
  private final ListingDocumentGenerator generator;
  private final UserRepository users;
  private final Path signedRoot;
  private final NotificationService notifications;
  private final AuditService audit;
  private final DocumentEmailService documentEmails;

  public ListingDocumentService(
      OwnerListingDocumentRepository documents,
      ListingDocumentGenerator generator,
      UserRepository users,
      NotificationService notifications,
      DocumentEmailService documentEmails,
      AuditService audit,
      @Value("${stowup.uploads.directory}") String directory) {
    this.documents = documents;
    this.generator = generator;
    this.users = users;
    this.signedRoot =
        Path.of(directory)
            .toAbsolutePath()
            .normalize()
            .resolve("listing-documents")
            .resolve("signed");
    this.notifications = notifications;
    this.documentEmails = documentEmails;
    this.audit = audit;
  }

  @Transactional
  public OwnerListingDocument createFor(Space space) {
    String generatedPath = generator.generate(space).toString();
    return documents
        .findBySpaceId(space.getId())
        .orElseGet(
            () -> {
              OwnerListingDocument created =
                  documents.save(new OwnerListingDocument(space, generatedPath));
              documentEmails.sendPdf(
                  space.getOwner().getEmail(),
                  "Termo do seu anúncio StowUp",
                  "Olá, "
                      + space.getOwner().getName()
                      + ".\n\nO termo do anúncio está anexado. Confira, assine pelo gov.br e envie o PDF assinado pelo aplicativo StowUp.",
                  Path.of(generatedPath),
                  "termo-anunciante-" + space.getId() + ".pdf");
              return created;
            });
  }

  @Transactional(readOnly = true)
  public OwnerListingDocument ownerDocument(long spaceId, String email) {
    OwnerListingDocument d = requireBySpace(spaceId);
    requireOwner(d, email);
    return d;
  }

  @Transactional(readOnly = true)
  public Path generatedPath(long spaceId, String email) {
    return Path.of(ownerDocument(spaceId, email).getGeneratedPdfPath());
  }

  @Transactional
  public OwnerListingDocument upload(long spaceId, String email, MultipartFile file) {
    OwnerListingDocument d = ownerDocument(spaceId, email);
    if (file.isEmpty() || file.getSize() > 15L * 1024 * 1024)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Envie um PDF assinado de até 15 MB.");
    String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    if (!"application/pdf".equalsIgnoreCase(file.getContentType())
        && !originalName.toLowerCase().endsWith(".pdf"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O documento precisa ser um PDF.");
    try {
      byte[] header = file.getInputStream().readNBytes(5);
      if (!new String(header).equals("%PDF-"))
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "O arquivo enviado não é um PDF válido.");
      Files.createDirectories(signedRoot);
      Path target = signedRoot.resolve(d.getId() + "-" + UUID.randomUUID() + ".pdf");
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
      d.signed(target.toString(), safeName(file.getOriginalFilename()));
      return d;
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível guardar o documento.");
    }
  }

  @Transactional(readOnly = true)
  public List<OwnerListingDocument> pending() {
    return documents.findByStatusOrderByCreatedAtAsc(ListingDocumentStatus.PENDING_REVIEW);
  }

  @Transactional(readOnly = true)
  public OwnerListingDocument requireStaff(String id) {
    return documents
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado."));
  }

  @Transactional(readOnly = true)
  public Path signedPath(String id) {
    OwnerListingDocument d = requireStaff(id);
    if (d.getSignedPdfPath() == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF assinado ainda não enviado.");
    return Path.of(d.getSignedPdfPath());
  }

  @Transactional
  public OwnerListingDocument review(
      String id,
      String reviewerEmail,
      boolean approved,
      boolean nameMatches,
      String reference,
      String notes) {
    if (approved && !nameMatches)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Confirme o nome do assinante antes de aprovar.");
    UserAccount reviewer =
        users
            .findByEmailIgnoreCase(reviewerEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    OwnerListingDocument d = requireStaff(id);
    d.review(approved, nameMatches, reference.trim(), notes.trim(), reviewer);
    UserAccount owner = d.getSpace().getOwner();
    if (approved) owner.setOwnerVerified(true);
    notifications.send(
        owner,
        approved ? "OWNER_VERIFIED" : "DOCUMENT_REJECTED",
        approved ? "Proprietário verificado" : "Correção necessária no documento",
        approved
            ? "Seus documentos foram aprovados e o selo de proprietário verificado foi liberado."
            : notes.trim(),
        "SPACE",
        d.getSpace().getId());
    audit.record(
        reviewerEmail,
        approved ? "OWNER_DOCUMENT_APPROVED" : "OWNER_DOCUMENT_REJECTED",
        "SPACE",
        d.getSpace().getId(),
        notes);
    return d;
  }

  @Transactional(readOnly = true)
  public boolean isApproved(long spaceId) {
    return documents
        .findBySpaceId(spaceId)
        .map(d -> d.getStatus() == ListingDocumentStatus.APPROVED)
        .orElse(false);
  }

  private OwnerListingDocument requireBySpace(long id) {
    return documents
        .findBySpaceId(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Termo do anunciante não encontrado."));
  }

  private void requireOwner(OwnerListingDocument d, String email) {
    if (!d.getSpace().getOwner().getEmail().equalsIgnoreCase(email))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  private String safeName(String value) {
    return value == null
        ? "termo-assinado.pdf"
        : Path.of(value).getFileName().toString().replaceAll("[^A-Za-z0-9._ -]", "_");
  }
}
