package br.com.stowup.admin;

import br.com.stowup.moderation.application.SpaceModerationService;
import br.com.stowup.space.Space;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/moderation/spaces")
public class StaffModerationController {
  private final SpaceModerationService moderation;

  public StaffModerationController(SpaceModerationService moderation) {
    this.moderation = moderation;
  }

  @GetMapping("/pending")
  @Transactional(readOnly = true)
  public List<ModerationSpaceView> pending() {
    return moderation.pending().stream().map(ModerationSpaceView::from).toList();
  }

  @PostMapping("/{id}/approve")
  @Transactional
  public ModerationSpaceView approve(@PathVariable Long id, Principal principal) {
    return ModerationSpaceView.from(moderation.approve(id, principal.getName()));
  }

  @PostMapping("/{id}/reject")
  @Transactional
  public ModerationSpaceView reject(
      @PathVariable Long id, @Valid @RequestBody RejectSpaceRequest request, Principal principal) {
    return ModerationSpaceView.from(moderation.reject(id, principal.getName(), request.reason()));
  }

  public record RejectSpaceRequest(@NotBlank @Size(min = 10, max = 500) String reason) {}

  public record ModerationSpaceView(
      Long id,
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
      String moderationReason,
      Instant moderatedAt) {
    static ModerationSpaceView from(Space space) {
      return new ModerationSpaceView(
          space.getId(),
          space.getTitle(),
          space.getOwner().getName(),
          space.getOwner().getEmail(),
          space.getCity(),
          space.getDistrict(),
          space.getType(),
          space.getSize(),
          space.getPrice(),
          space.getDescription(),
          space.getPhotoUrls(),
          space.getModerationStatus(),
          space.getModerationReason(),
          space.getModeratedAt());
    }
  }
}
