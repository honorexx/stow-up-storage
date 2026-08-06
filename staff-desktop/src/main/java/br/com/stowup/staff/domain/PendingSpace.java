package br.com.stowup.staff.domain;

import java.math.BigDecimal;
import java.util.List;

public record PendingSpace(
    long id,
    String title,
    String ownerName,
    String ownerEmail,
    String city,
    String district,
    String type,
    int size,
    BigDecimal price,
    String description,
    List<String> photos,
    String moderationStatus,
    String moderationReason) {
  @Override
  public String toString() {
    return title + "\n" + ownerName + "  ·  " + city + "/" + district;
  }
}
