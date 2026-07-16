package br.com.stowup.conversation;

import br.com.stowup.notification.NotificationService;
import br.com.stowup.space.Space;
import br.com.stowup.space.SpaceRepository;
import br.com.stowup.user.UserAccount;
import br.com.stowup.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/conversations")
public class RentalConversationController {
  private final RentalConversationRepository conversations;
  private final RentalMessageRepository messages;
  private final SpaceRepository spaces;
  private final UserRepository users;
  private final NotificationService notifications;

  public RentalConversationController(
      RentalConversationRepository conversations,
      RentalMessageRepository messages,
      SpaceRepository spaces,
      UserRepository users,
      NotificationService notifications) {
    this.conversations = conversations;
    this.messages = messages;
    this.spaces = spaces;
    this.users = users;
    this.notifications = notifications;
  }

  @GetMapping
  @Transactional(readOnly = true)
  public List<ConversationView> list(Principal principal) {
    return conversations
        .findByCustomerEmailIgnoreCaseOrOwnerEmailIgnoreCaseOrderByUpdatedAtDesc(
            principal.getName(), principal.getName())
        .stream()
        .map(item -> view(item, principal.getName()))
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public ConversationView start(@Valid @RequestBody StartRequest request, Principal principal) {
    Space space =
        spaces
            .findById(request.spaceId())
            .filter(Space::isAvailable)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anúncio não encontrado."));
    UserAccount customer =
        users
            .findByEmailIgnoreCase(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if (space.getOwner().getEmail().equalsIgnoreCase(customer.getEmail()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Você não pode iniciar uma conversa com seu próprio anúncio.");
    RentalConversation conversation =
        conversations
            .findBySpaceIdAndCustomerEmailIgnoreCase(space.getId(), customer.getEmail())
            .orElseGet(
                () ->
                    conversations.save(new RentalConversation(space, customer, space.getOwner())));
    return view(conversation, principal.getName());
  }

  @GetMapping("/{id}/messages")
  @Transactional(readOnly = true)
  public List<MessageView> messages(@PathVariable String id, Principal principal) {
    RentalConversation conversation = allowed(id, principal.getName());
    return messages.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
        .map(item -> MessageView.from(item, principal.getName()))
        .toList();
  }

  @PostMapping("/{id}/messages")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public MessageView send(
      @PathVariable String id, @Valid @RequestBody SendRequest request, Principal principal) {
    RentalConversation conversation = allowed(id, principal.getName());
    UserAccount sender =
        users
            .findByEmailIgnoreCase(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    conversation.touch();
    RentalMessage message = messages.save(new RentalMessage(conversation, sender, request.body()));
    UserAccount recipient = conversation.other(sender.getEmail());
    notifications.send(
        recipient,
        "RENTAL_MESSAGE",
        "Nova mensagem sobre " + conversation.getSpace().getTitle(),
        sender.getName() + ": " + request.body().trim(),
        "CONVERSATION",
        conversation.getId());
    return MessageView.from(message, principal.getName());
  }

  private RentalConversation allowed(String id, String email) {
    return conversations
        .findAccessible(id, email)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Você não participa desta conversa."));
  }

  private ConversationView view(RentalConversation item, String email) {
    String last =
        messages
            .findFirstByConversationIdOrderByCreatedAtDesc(item.getId())
            .map(RentalMessage::getBody)
            .orElse("Conversa iniciada");
    UserAccount other = item.other(email);
    return new ConversationView(
        item.getId(),
        item.getSpace().getId(),
        item.getSpace().getTitle(),
        other.getName(),
        other.getPhotoUrl(),
        last,
        item.getUpdatedAt());
  }

  public record StartRequest(@NotNull Long spaceId) {}

  public record SendRequest(@NotBlank @Size(max = 2000) String body) {}

  public record ConversationView(
      String id,
      Long spaceId,
      String spaceTitle,
      String participantName,
      String participantPhotoUrl,
      String lastMessage,
      Instant updatedAt) {}

  public record MessageView(
      Long id, String senderName, boolean mine, String body, Instant createdAt) {
    static MessageView from(RentalMessage message, String email) {
      return new MessageView(
          message.getId(),
          message.getSender().getName(),
          message.getSender().getEmail().equalsIgnoreCase(email),
          message.getBody(),
          message.getCreatedAt());
    }
  }
}
