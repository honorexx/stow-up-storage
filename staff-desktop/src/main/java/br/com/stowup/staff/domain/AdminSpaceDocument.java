package br.com.stowup.staff.domain;

import java.time.Instant;

public record AdminSpaceDocument(
    String id, String type, String name, String visibility, Instant uploadedAt) {
  @Override
  public String toString() {
    return name + "\n" + type + " · " + visibility;
  }
}
