package br.com.stowup.support;

import br.com.stowup.notification.NotificationService;
import br.com.stowup.user.UserAccount;
import br.com.stowup.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/staff/support")
public class StaffSupportController {
  private final SupportConversationRepository conversations;
  private final SupportMessageRepository messages;
  private final UserRepository users;
  private final SupportAttachmentStorage attachments;
  private final NotificationService notifications;

  public StaffSupportController(
      SupportConversationRepository conversations,
      SupportMessageRepository messages,
      UserRepository users,
      SupportAttachmentStorage attachments,
      NotificationService notifications) {
    this.conversations = conversations;
    this.messages = messages;
    this.users = users;
    this.attachments = attachments;
    this.notifications = notifications;
  }

  @GetMapping("/conversations")
  @Transactional(readOnly = true)
  public List<ConversationView> queue(@RequestParam(defaultValue = "ALL") String status) {
    return conversations.findAllByOrderByUpdatedAtDesc().stream()
        .filter(item -> status.equalsIgnoreCase("ALL") || item.getStatus().equalsIgnoreCase(status))
        .map(this::view)
        .toList();
  }

  @GetMapping("/conversations/{id}/messages")
  @Transactional(readOnly = true)
  public List<MessageView> history(@PathVariable String id) {
    requireConversation(id);
    return messages.findByConversationIdOrderByCreatedAtAsc(id).stream()
        .map(MessageView::from)
        .toList();
  }

  @PostMapping("/conversations/{id}/messages")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public MessageView reply(
      @PathVariable String id, @Valid @RequestBody ReplyRequest request, Principal principal) {
    SupportConversation conversation = requireConversation(id);
    UserAccount staff = requireStaff(principal);
    conversation.reopen();
    SupportMessage message =
        messages.save(SupportMessage.fromStaff(conversation, staff, request.body()));
    notifyCustomer(conversation, staff, message.getBody());
    return MessageView.from(message);
  }

  @PostMapping(
      value = "/conversations/{id}/attachments",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public MessageView attachment(
      @PathVariable String id, @RequestPart("file") MultipartFile file, Principal principal)
      throws IOException {
    SupportConversation conversation = requireConversation(id);
    UserAccount staff = requireStaff(principal);
    conversation.reopen();
    SupportMessage message =
        messages.save(SupportMessage.attachment(conversation, staff, true, attachments.save(file)));
    notifyCustomer(conversation, staff, message.getBody());
    return MessageView.from(message);
  }

  @GetMapping("/conversations/{id}/messages/{messageId}/attachment")
  @Transactional(readOnly = true)
  public ResponseEntity<FileSystemResource> downloadAttachment(
      @PathVariable String id, @PathVariable Long messageId) {
    requireConversation(id);
    SupportMessage message =
        messages
            .findById(messageId)
            .filter(item -> item.getConversation().getId().equals(id))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anexo não encontrado."));
    if (message.getAttachmentUrl() == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta mensagem não possui anexo.");
    FileSystemResource resource =
        new FileSystemResource(attachments.require(message.getAttachmentUrl()));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(message.getAttachmentContentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(message.getAttachmentName())
                .build()
                .toString())
        .body(resource);
  }

  @PatchMapping("/conversations/{id}/status")
  @Transactional
  public ConversationView status(
      @PathVariable String id, @Valid @RequestBody StatusRequest request) {
    SupportConversation conversation = requireConversation(id);
    if (request.status().equals("CLOSED")) conversation.close();
    else conversation.reopen();
    return view(conversation);
  }

  private UserAccount requireStaff(Principal principal) {
    return users
        .findByEmailIgnoreCase(principal.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  private SupportConversation requireConversation(String id) {
    return conversations
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atendimento não encontrado."));
  }

  private void notifyCustomer(SupportConversation conversation, UserAccount staff, String message) {
    notifications.send(
        conversation.getCustomer(),
        "SUPPORT_MESSAGE",
        "Nova resposta do suporte",
        staff.getName() + ": " + message,
        "SUPPORT_TICKET",
        conversation.getId());
  }

  private ConversationView view(SupportConversation item) {
    String last =
        messages.findByConversationIdOrderByCreatedAtAsc(item.getId()).stream()
            .reduce((a, b) -> b)
            .map(SupportMessage::getBody)
            .orElse("Atendimento iniciado");
    return new ConversationView(
        item.getId(),
        item.getTicketNumber(),
        item.getCategory(),
        item.getSubject(),
        item.getCustomer().getName(),
        item.getCustomer().getEmail(),
        item.getStatus(),
        last,
        item.getUpdatedAt());
  }

  public record ReplyRequest(@NotBlank @Size(max = 2000) String body) {}

  public record StatusRequest(@NotBlank @Pattern(regexp = "OPEN|CLOSED") String status) {}

  public record ConversationView(
      String id,
      String ticketNumber,
      SupportCategory category,
      String subject,
      String customerName,
      String customerEmail,
      String status,
      String lastMessage,
      Instant updatedAt) {}

  public record MessageView(
      Long id,
      String senderType,
      String senderName,
      String body,
      String attachmentName,
      String attachmentContentType,
      Long attachmentSize,
      Instant createdAt) {
    static MessageView from(SupportMessage message) {
      return new MessageView(
          message.getId(),
          message.getSenderType(),
          message.getSender() == null ? "Sistema" : message.getSender().getName(),
          message.getBody(),
          message.getAttachmentName(),
          message.getAttachmentContentType(),
          message.getAttachmentSize(),
          message.getCreatedAt());
    }
  }
}
