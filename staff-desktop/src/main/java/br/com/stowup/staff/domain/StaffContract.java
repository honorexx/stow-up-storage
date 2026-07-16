package br.com.stowup.staff.domain;

import java.time.Instant;

public record StaffContract(
    String id,
    String bookingId,
    String contractCode,
    String status,
    String customerName,
    String customerEmail,
    String ownerName,
    String spaceTitle,
    String originalFilename,
    String validatorUrl,
    Instant signedAt) {
  @Override
  public String toString() {
    return contractCode + "  ·  " + status + "\n" + customerName + "  ·  " + spaceTitle;
  }
}
