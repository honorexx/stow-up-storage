package br.com.stowup.listingdocument.domain;

import br.com.stowup.space.Space;
import br.com.stowup.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "owner_listing_documents")
public class OwnerListingDocument {
  @Id private String id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "space_id", nullable = false, unique = true)
  private Space space;

  @Column(name = "document_code", nullable = false, unique = true, length = 30)
  private String documentCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private ListingDocumentStatus status;

  @Column(name = "generated_pdf_path", nullable = false, length = 500)
  private String generatedPdfPath;

  @Column(name = "signed_pdf_path", length = 500)
  private String signedPdfPath;

  @Column(name = "original_filename", length = 255)
  private String originalFilename;

  @Column(name = "review_notes", length = 1000)
  private String reviewNotes;

  @Column(name = "validator_reference", length = 500)
  private String validatorReference;

  @Column(name = "signer_name_matches")
  private Boolean signerNameMatches;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "signed_at")
  private Instant signedAt;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by")
  private UserAccount reviewedBy;

  protected OwnerListingDocument() {}

  public OwnerListingDocument(Space space, String generatedPdfPath) {
    this.id = UUID.randomUUID().toString();
    this.space = space;
    this.documentCode = "DOC-SU-" + space.getId();
    this.status = ListingDocumentStatus.GENERATED;
    this.generatedPdfPath = generatedPdfPath;
    this.createdAt = Instant.now();
  }

  public void signed(String path, String filename) {
    signedPdfPath = path;
    originalFilename = filename;
    status = ListingDocumentStatus.PENDING_REVIEW;
    signedAt = Instant.now();
    reviewedAt = null;
    reviewedBy = null;
    reviewNotes = null;
    validatorReference = null;
    signerNameMatches = null;
  }

  public void review(
      boolean approved, boolean nameMatches, String reference, String notes, UserAccount reviewer) {
    status = approved ? ListingDocumentStatus.APPROVED : ListingDocumentStatus.REJECTED;
    signerNameMatches = nameMatches;
    validatorReference = reference;
    reviewNotes = notes;
    reviewedBy = reviewer;
    reviewedAt = Instant.now();
  }

  public String getId() {
    return id;
  }

  public Space getSpace() {
    return space;
  }

  public String getDocumentCode() {
    return documentCode;
  }

  public ListingDocumentStatus getStatus() {
    return status;
  }

  public String getGeneratedPdfPath() {
    return generatedPdfPath;
  }

  public String getSignedPdfPath() {
    return signedPdfPath;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public String getReviewNotes() {
    return reviewNotes;
  }

  public String getValidatorReference() {
    return validatorReference;
  }

  public Boolean getSignerNameMatches() {
    return signerNameMatches;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getSignedAt() {
    return signedAt;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }
}
