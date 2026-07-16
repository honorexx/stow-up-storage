package br.com.stowup.support;

import br.com.stowup.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "support_messages")
public class SupportMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conversation_id", nullable = false)
  private SupportConversation conversation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id")
  private UserAccount sender;

  @Column(nullable = false, length = 20)
  private String senderType;

  @Column(nullable = false, length = 2000)
  private String body;

  @Column(name = "attachment_url", length = 500)
  private String attachmentUrl;

  @Column(name = "attachment_name", length = 240)
  private String attachmentName;

  @Column(name = "attachment_content_type", length = 120)
  private String attachmentContentType;

  @Column(name = "attachment_size")
  private Long attachmentSize;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected SupportMessage() {}

  public SupportMessage(SupportConversation conversation, UserAccount sender, String body) {
    this.conversation = conversation;
    this.sender = sender;
    this.senderType = "CUSTOMER";
    this.body = body.trim();
  }

  public static SupportMessage fromStaff(
      SupportConversation conversation, UserAccount sender, String body) {
    SupportMessage message = new SupportMessage(conversation, sender, body);
    message.senderType = "SUPPORT";
    return message;
  }

  public static SupportMessage attachment(
      SupportConversation conversation,
      UserAccount sender,
      boolean fromStaff,
      SupportAttachmentStorage.StoredAttachment attachment) {
    SupportMessage message =
        new SupportMessage(conversation, sender, "Anexo: " + attachment.originalName());
    message.senderType = fromStaff ? "SUPPORT" : "CUSTOMER";
    message.attachmentUrl = attachment.storedName();
    message.attachmentName = attachment.originalName();
    message.attachmentContentType = attachment.contentType();
    message.attachmentSize = attachment.size();
    return message;
  }

  public Long getId() {
    return id;
  }

  public String getSenderType() {
    return senderType;
  }

  public String getBody() {
    return body;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public UserAccount getSender() {
    return sender;
  }

  public SupportConversation getConversation() {
    return conversation;
  }

  public String getAttachmentUrl() {
    return attachmentUrl;
  }

  public String getAttachmentName() {
    return attachmentName;
  }

  public String getAttachmentContentType() {
    return attachmentContentType;
  }

  public Long getAttachmentSize() {
    return attachmentSize;
  }
}
