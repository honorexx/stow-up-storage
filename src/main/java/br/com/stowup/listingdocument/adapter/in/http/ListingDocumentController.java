package br.com.stowup.listingdocument.adapter.in.http;

import br.com.stowup.listingdocument.application.ListingDocumentService;
import br.com.stowup.listingdocument.domain.OwnerListingDocument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ListingDocumentController {
  private final ListingDocumentService service;

  public ListingDocumentController(ListingDocumentService service) {
    this.service = service;
  }

  @GetMapping("/api/owner/spaces/{spaceId}/document")
  public DocumentView owner(@PathVariable long spaceId, Principal p) {
    return DocumentView.from(service.ownerDocument(spaceId, p.getName()));
  }

  @GetMapping("/api/owner/spaces/{spaceId}/document/pdf")
  public ResponseEntity<Resource> generated(@PathVariable long spaceId, Principal p) {
    return pdf(service.generatedPath(spaceId, p.getName()), "termo-anunciante-" + spaceId + ".pdf");
  }

  @PostMapping(
      value = "/api/owner/spaces/{spaceId}/document/signed",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DocumentView upload(
      @PathVariable long spaceId, @RequestPart("file") MultipartFile file, Principal p) {
    return DocumentView.from(service.upload(spaceId, p.getName(), file));
  }

  @GetMapping("/api/staff/listing-documents")
  public List<StaffDocumentView> pending() {
    return service.pending().stream().map(StaffDocumentView::from).toList();
  }

  @GetMapping("/api/staff/listing-documents/{id}/signed")
  public ResponseEntity<Resource> signed(@PathVariable String id) {
    OwnerListingDocument d = service.requireStaff(id);
    return pdf(service.signedPath(id), d.getOriginalFilename());
  }

  @PostMapping("/api/staff/listing-documents/{id}/review")
  public StaffDocumentView review(
      @PathVariable String id, @Valid @RequestBody ReviewRequest r, Principal p) {
    return StaffDocumentView.from(
        service.review(
            id,
            p.getName(),
            r.approved(),
            r.signerNameMatches(),
            r.validatorReference(),
            r.notes()));
  }

  private ResponseEntity<Resource> pdf(java.nio.file.Path path, String filename) {
    Resource resource = new FileSystemResource(path);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename=\"" + filename.replace("\"", "") + "\"")
        .body(resource);
  }

  public record ReviewRequest(
      boolean approved,
      boolean signerNameMatches,
      @NotBlank @Size(max = 500) String validatorReference,
      @NotBlank @Size(min = 5, max = 1000) String notes) {}

  public record DocumentView(
      String id,
      long spaceId,
      String documentCode,
      String status,
      String originalFilename,
      String reviewNotes,
      Instant createdAt,
      Instant signedAt,
      Instant reviewedAt) {
    static DocumentView from(OwnerListingDocument d) {
      return new DocumentView(
          d.getId(),
          d.getSpace().getId(),
          d.getDocumentCode(),
          d.getStatus().name(),
          d.getOriginalFilename(),
          d.getReviewNotes(),
          d.getCreatedAt(),
          d.getSignedAt(),
          d.getReviewedAt());
    }
  }

  public record StaffDocumentView(
      String id,
      long spaceId,
      String documentCode,
      String status,
      String ownerName,
      String ownerEmail,
      String spaceTitle,
      String originalFilename,
      Instant signedAt) {
    static StaffDocumentView from(OwnerListingDocument d) {
      return new StaffDocumentView(
          d.getId(),
          d.getSpace().getId(),
          d.getDocumentCode(),
          d.getStatus().name(),
          d.getSpace().getOwner().getName(),
          d.getSpace().getOwner().getEmail(),
          d.getSpace().getTitle(),
          d.getOriginalFilename(),
          d.getSignedAt());
    }
  }
}
