package br.com.stowup.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupportAttachmentStorage {
  private static final Set<String> ALLOWED =
      Set.of(
          "application/pdf",
          "text/plain",
          "image/jpeg",
          "image/png",
          "image/webp",
          "image/heic",
          "application/zip",
          "application/msword",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
  private final Path directory;

  public SupportAttachmentStorage(@Value("${stowup.uploads.directory}") String uploadRoot) {
    directory = Path.of(uploadRoot).toAbsolutePath().normalize().resolve("support");
  }

  public StoredAttachment save(MultipartFile file) throws IOException {
    if (file.isEmpty() || file.getSize() > 15 * 1024L * 1024L)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O anexo deve ter até 15 MB.");
    String contentType =
        file.getContentType() == null ? "application/octet-stream" : file.getContentType();
    if (!ALLOWED.contains(contentType))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Envie PDF, foto, texto, ZIP ou documento do Word.");
    Files.createDirectories(directory);
    String original = safeName(file.getOriginalFilename());
    String stored = UUID.randomUUID() + extension(original);
    Path destination = directory.resolve(stored).normalize();
    Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
    return new StoredAttachment(stored, original, contentType, file.getSize());
  }

  public Path require(String storedName) {
    Path path = directory.resolve(storedName).normalize();
    if (!path.startsWith(directory) || !Files.isRegularFile(path))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Anexo não encontrado.");
    return path;
  }

  private static String safeName(String value) {
    String name =
        value == null || value.isBlank() ? "anexo" : value.replaceAll("[\\\\/:*?\"<>|]", "_");
    return name.length() > 240 ? name.substring(name.length() - 240) : name;
  }

  private static String extension(String name) {
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot).toLowerCase();
  }

  public record StoredAttachment(
      String storedName, String originalName, String contentType, long size) {}
}
