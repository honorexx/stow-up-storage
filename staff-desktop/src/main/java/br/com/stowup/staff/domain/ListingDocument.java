package br.com.stowup.staff.domain;

import java.time.Instant;

public record ListingDocument(
    String id,
    long spaceId,
    String documentCode,
    String status,
    String ownerName,
    String ownerEmail,
    String spaceTitle,
    String originalFilename,
    Instant signedAt) {
  @Override
  public String toString() {
    return documentCode + " · " + spaceTitle + "\n" + ownerName + " · " + status;
  }
}
