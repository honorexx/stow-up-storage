package br.com.stowup.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImageStorage {
  private static final Map<String, String> EXTENSIONS =
      Map.of(
          "image/jpeg", ".jpg",
          "image/jpg", ".jpg",
          "image/png", ".png",
          "image/webp", ".webp",
          "image/heic", ".heic",
          "image/heif", ".heif");
  private final Path root;

  public ImageStorage(@Value("${stowup.uploads.directory}") String directory) {
    root = Path.of(directory).toAbsolutePath().normalize();
  }

  public String saveProfile(MultipartFile file) throws IOException {
    return save(file, root.resolve("profiles"), "/uploads/profiles/", 5);
  }

  public String saveSpace(Long spaceId, MultipartFile file) throws IOException {
    return save(
        file,
        root.resolve("spaces").resolve(String.valueOf(spaceId)),
        "/uploads/spaces/" + spaceId + "/",
        8);
  }

  private String save(MultipartFile file, Path directory, String publicPrefix, int maximumMegabytes)
      throws IOException {
    if (file.isEmpty() || file.getSize() > maximumMegabytes * 1024L * 1024L) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A imagem deve ter até " + maximumMegabytes + " MB.");
    }
    String contentType =
        file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    String extension = EXTENSIONS.get(contentType);
    if (extension == null) extension = extensionFromFilename(file.getOriginalFilename());
    if (extension == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use uma imagem JPG, PNG, WebP ou HEIC.");
    }
    Files.createDirectories(directory);
    String filename = UUID.randomUUID() + extension;
    Path destination = directory.resolve(filename).normalize();
    if (!destination.startsWith(directory)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido.");
    }
    Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
    return publicPrefix + filename;
  }

  private String extensionFromFilename(String filename) {
    if (filename == null) return null;
    String value = filename.toLowerCase(Locale.ROOT);
    if (value.endsWith(".jpg") || value.endsWith(".jpeg")) return ".jpg";
    if (value.endsWith(".png")) return ".png";
    if (value.endsWith(".webp")) return ".webp";
    if (value.endsWith(".heic")) return ".heic";
    if (value.endsWith(".heif")) return ".heif";
    return null;
  }
}
