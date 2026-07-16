package br.com.stowup.staff.domain;

import java.time.Instant;

public record SupportMessage(
    long id,
    String senderType,
    String senderName,
    String body,
    String attachmentUrl,
    String attachmentName,
    String attachmentContentType,
    Long attachmentSize,
    Instant createdAt) {}
