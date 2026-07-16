package br.com.stowup.staff.domain;

import java.time.Instant;

public record SupportTicket(
    String id,
    String ticketNumber,
    String category,
    String subject,
    String customerName,
    String customerEmail,
    String status,
    String lastMessage,
    Instant updatedAt) {
  @Override
  public String toString() {
    return ticketNumber + "  ·  " + subject + "\n" + customerName + "  ·  " + status;
  }
}
