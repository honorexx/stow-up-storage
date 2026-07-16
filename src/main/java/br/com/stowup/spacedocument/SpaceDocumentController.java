package br.com.stowup.spacedocument;

import br.com.stowup.audit.AuditService;
import br.com.stowup.space.*;
import java.nio.file.*;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/owner/spaces/{spaceId}/documents")
public class SpaceDocumentController {
  private final SpaceDocumentRepository documents;
  private final SpaceRepository spaces;
  private final AuditService audit;
  private final Path root;

  public SpaceDocumentController(
      SpaceDocumentRepository d,
      SpaceRepository s,
      AuditService a,
      @Value("${stowup.uploads.directory}") String dir) {
    documents = d;
    spaces = s;
    audit = a;
    root = Path.of(dir).toAbsolutePath().normalize().resolve("space-documents");
  }

  @GetMapping
  @Transactional(readOnly = true)
  public List<View> list(@PathVariable Long spaceId, Principal p) {
    owner(spaceId, p);
    return documents.findBySpaceIdOrderByUploadedAtDesc(spaceId).stream().map(View::from).toList();
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public View upload(
      @PathVariable Long spaceId,
      @RequestPart("file") MultipartFile file,
      @RequestParam String type,
      @RequestParam(defaultValue = "ADMIN_ONLY") String visibility,
      Principal p)
      throws Exception {
    Space s = owner(spaceId, p);
    if (file.isEmpty() || file.getSize() > 15L * 1024 * 1024)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Documento inválido ou maior que 15 MB.");
    String name =
        Path.of(file.getOriginalFilename() == null ? "documento.pdf" : file.getOriginalFilename())
            .getFileName()
            .toString();
    Path dir = root.resolve(String.valueOf(spaceId));
    Files.createDirectories(dir);
    Path target =
        dir.resolve(java.util.UUID.randomUUID() + "-" + name.replaceAll("[^A-Za-z0-9._-]", "_"));
    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    SpaceDocument saved =
        documents.save(new SpaceDocument(s, type, name, target.toString(), visibility));
    audit.record(p.getName(), "SPACE_DOCUMENT_UPLOADED", "SPACE", spaceId, type);
    return View.from(saved);
  }

  private Space owner(Long id, Principal p) {
    return spaces
        .findByIdAndOwnerEmailIgnoreCase(id, p.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  public record View(String id, String type, String name, String visibility, Instant uploadedAt) {
    static View from(SpaceDocument d) {
      return new View(
          d.getId(), d.getDocumentType(), d.getDisplayName(), d.getVisibility(), d.getUploadedAt());
    }
  }
}
