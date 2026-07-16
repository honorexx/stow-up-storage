package br.com.stowup.staff.domain;

import java.math.BigDecimal;
import java.util.List;

public record ManagedSpace(
    long id,
    String title,
    String ownerName,
    String ownerEmail,
    long ownerId,
    String city,
    String district,
    String postalCode,
    String addressLine,
    String addressNumber,
    String addressComplement,
    String state,
    String type,
    int size,
    BigDecimal heightMeters,
    BigDecimal lengthMeters,
    BigDecimal depthMeters,
    BigDecimal volumeCubicMeters,
    int capacityTotal,
    int capacityUsed,
    BigDecimal price,
    String description,
    boolean available,
    boolean verified,
    String moderationStatus,
    String lifecycleStatus,
    String moderationReason,
    List<String> photos,
    long documentCount,
    String listingDocumentId,
    String listingDocumentStatus,
    String listingDocumentFilename) {
  @Override
  public String toString() {
    return "#" + id + " · " + title + "\n" + ownerName + " · " + city + " · " + lifecycleStatus;
  }
}
