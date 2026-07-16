package br.com.stowup.spacedocument;

import br.com.stowup.space.Space;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "space_documents")
public class SpaceDocument {
  @Id
  @Column(length = 36)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "space_id", nullable = false)
  private Space space;

  @Column(name = "document_type", nullable = false, length = 40)
  private String documentType;

  @Column(name = "display_name", nullable = false, length = 180)
  private String displayName;

  @Column(name = "file_path", nullable = false, length = 600)
  private String filePath;

  @Column(nullable = false, length = 20)
  private String visibility;

  @Column(name = "uploaded_at", nullable = false, updatable = false)
  private Instant uploadedAt = Instant.now();

  protected SpaceDocument() {}

  public SpaceDocument(Space s, String type, String name, String path, String visibility) {
    id = UUID.randomUUID().toString();
    space = s;
    documentType = type;
    displayName = name;
    filePath = path;
    this.visibility = visibility;
  }

  public String getId() {
    return id;
  }

  public Space getSpace() {
    return space;
  }

  public String getDocumentType() {
    return documentType;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getFilePath() {
    return filePath;
  }

  public String getVisibility() {
    return visibility;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }
}
