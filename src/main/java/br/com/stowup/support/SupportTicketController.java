package br.com.stowup.support;

import br.com.stowup.user.UserAccount;
import br.com.stowup.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/support/tickets")
public class SupportTicketController {
  private final SupportConversationRepository tickets;
  private final SupportMessageRepository messages;
  private final UserRepository users;
  private final SupportAttachmentStorage attachments;

  public SupportTicketController(
      SupportConversationRepository tickets,
      SupportMessageRepository messages,
      UserRepository users,
      SupportAttachmentStorage attachments) {
    this.tickets = tickets;
    this.messages = messages;
    this.users = users;
    this.attachments = attachments;
  }

  @GetMapping
  @Transactional(readOnly = true)
  public List<TicketView> list(Principal principal) {
    return tickets.findByCustomerEmailIgnoreCaseOrderByUpdatedAtDesc(principal.getName()).stream()
        .map(this::view)
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public TicketView create(@Valid @RequestBody CreateTicketRequest request, Principal principal) {
    UserAccount customer = requireUser(principal);
    SupportConversation ticket =
        tickets.save(new SupportConversation(customer, request.category(), request.subject()));
    messages.save(new SupportMessage(ticket, customer, request.description()));
    ticket.touch();
    return view(ticket);
  }

  @GetMapping("/{id}/messages")
  @Transactional(readOnly = true)
  public List<MessageView> history(@PathVariable String id, Principal principal) {
    requireOwned(id, principal);
    return messages.findByConversationIdOrderByCreatedAtAsc(id).stream()
        .map(MessageView::from)
        .toList();
  }

  @PostMapping("/{id}/messages")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public MessageView reply(
      @PathVariable String id, @Valid @RequestBody ReplyRequest request, Principal principal) {
    SupportConversation ticket = requireOwned(id, principal);
    if ("CLOSED".equals(ticket.getStatus()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Este chamado foi encerrado. Abra um novo chamado se precisar de ajuda.");
    ticket.touch();
    return MessageView.from(
        messages.save(new SupportMessage(ticket, requireUser(principal), request.body())));
  }

  @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public MessageView attachment(
      @PathVariable String id, @RequestPart("file") MultipartFile file, Principal principal)
      throws IOException {
    SupportConversation ticket = requireOwned(id, principal);
    if ("CLOSED".equals(ticket.getStatus())) ticket.reopen();
    SupportMessage message =
        SupportMessage.attachment(ticket, requireUser(principal), false, attachments.save(file));
    return MessageView.from(messages.save(message));
  }

  @GetMapping("/{id}/messages/{messageId}/attachment")
  @Transactional(readOnly = true)
  public ResponseEntity<FileSystemResource> downloadAttachment(
      @PathVariable String id, @PathVariable Long messageId, Principal principal) {
    requireOwned(id, principal);
    return attachmentResponse(requireMessage(id, messageId));
  }

  private UserAccount requireUser(Principal principal) {
    return users
        .findByEmailIgnoreCase(principal.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  private SupportConversation requireOwned(String id, Principal principal) {
    return tickets
        .findByIdAndCustomerEmailIgnoreCase(id, principal.getName())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado."));
  }

  private SupportMessage requireMessage(String conversationId, Long messageId) {
    SupportMessage message =
        messages
            .findById(messageId)
            .filter(item -> item.getConversation().getId().equals(conversationId))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anexo não encontrado."));
    if (message.getAttachmentUrl() == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta mensagem não possui anexo.");
    return message;
  }

  private ResponseEntity<FileSystemResource> attachmentResponse(SupportMessage message) {
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

  private TicketView view(SupportConversation ticket) {
    String lastMessage =
        messages.findByConversationIdOrderByCreatedAtAsc(ticket.getId()).stream()
            .reduce((first, last) -> last)
            .map(SupportMessage::getBody)
            .orElse("");
    return new TicketView(
        ticket.getId(),
        ticket.getTicketNumber(),
        ticket.getCategory(),
        ticket.getSubject(),
        ticket.getStatus(),
        lastMessage,
        ticket.getCreatedAt(),
        ticket.getUpdatedAt());
  }

  public record CreateTicketRequest(
      @NotNull SupportCategory category,
      @NotBlank @Size(min = 4, max = 140) String subject,
      @NotBlank @Size(min = 10, max = 2000) String description) {}

  public record ReplyRequest(@NotBlank @Size(max = 2000) String body) {}

  public record TicketView(
      String id,
      String ticketNumber,
      SupportCategory category,
      String subject,
      String status,
      String lastMessage,
      Instant createdAt,
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
