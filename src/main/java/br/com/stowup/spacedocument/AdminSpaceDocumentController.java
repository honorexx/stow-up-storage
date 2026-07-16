package br.com.stowup.spacedocument;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminSpaceDocumentController {
  private final SpaceDocumentRepository documents;

  public AdminSpaceDocumentController(SpaceDocumentRepository documents) {
    this.documents = documents;
  }

  @GetMapping("/spaces/{spaceId}/documents")
  @Transactional(readOnly = true)
  public List<DocumentView> list(@PathVariable Long spaceId) {
    return documents.findBySpaceIdOrderByUploadedAtDesc(spaceId).stream()
        .map(DocumentView::from)
        .toList();
  }

  @GetMapping("/space-documents/{id}/download")
  @Transactional(readOnly = true)
  public ResponseEntity<FileSystemResource> download(@PathVariable String id) {
    SpaceDocument document =
        documents
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Documento não encontrado."));
    FileSystemResource file = new FileSystemResource(document.getFilePath());
    if (!file.exists())
      throw new ResponseStatusException(NOT_FOUND, "Arquivo não encontrado no armazenamento.");
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(document.getDisplayName(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(file);
  }

  public record DocumentView(
      String id, String type, String name, String visibility, Instant uploadedAt) {
    static DocumentView from(SpaceDocument document) {
      return new DocumentView(
          document.getId(),
          document.getDocumentType(),
          document.getDisplayName(),
          document.getVisibility(),
          document.getUploadedAt());
    }
  }
}
