package br.com.stowup.contract;

import br.com.stowup.booking.Booking;
import br.com.stowup.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rental_contracts")
public class RentalContract {
  @Id
  @Column(length = 36)
  private String id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false, unique = true)
  private Booking booking;

  @Column(nullable = false, length = 30)
  private String status = "GENERATED";

  @Column(name = "generated_path", nullable = false, length = 600)
  private String generatedPath;

  @Column(name = "signed_path", length = 600)
  private String signedPath;

  @Column(name = "original_signed_filename", length = 255)
  private String originalSignedFilename;

  @Column(name = "validator_report_reference", length = 500)
  private String validatorReportReference;

  @Column(name = "review_notes", length = 1000)
  private String reviewNotes;

  private Boolean signerNameMatches;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by")
  private UserAccount reviewedBy;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  private Instant signedAt;
  private Instant reviewedAt;

  protected RentalContract() {}

  public RentalContract(Booking booking, String generatedPath) {
    this.id = UUID.randomUUID().toString();
    this.booking = booking;
    this.generatedPath = generatedPath;
  }

  public void signed(String path, String filename) {
    signedPath = path;
    originalSignedFilename = filename;
    status = "PENDING_REVIEW";
    signedAt = Instant.now();
    reviewerReset();
  }

  public void approve(UserAccount reviewer, String validatorReference, String notes) {
    status = "APPROVED";
    reviewedBy = reviewer;
    validatorReportReference = validatorReference;
    reviewNotes = notes;
    signerNameMatches = true;
    reviewedAt = Instant.now();
  }

  public void reject(
      UserAccount reviewer, boolean nameMatches, String validatorReference, String notes) {
    status = "REJECTED";
    reviewedBy = reviewer;
    validatorReportReference = validatorReference;
    reviewNotes = notes;
    signerNameMatches = nameMatches;
    reviewedAt = Instant.now();
  }

  private void reviewerReset() {
    reviewedBy = null;
    validatorReportReference = null;
    reviewNotes = null;
    signerNameMatches = null;
    reviewedAt = null;
  }

  public String getId() {
    return id;
  }

  public Booking getBooking() {
    return booking;
  }

  public String getStatus() {
    return status;
  }

  public String getGeneratedPath() {
    return generatedPath;
  }

  public String getSignedPath() {
    return signedPath;
  }

  public String getOriginalSignedFilename() {
    return originalSignedFilename;
  }

  public String getValidatorReportReference() {
    return validatorReportReference;
  }

  public String getReviewNotes() {
    return reviewNotes;
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
