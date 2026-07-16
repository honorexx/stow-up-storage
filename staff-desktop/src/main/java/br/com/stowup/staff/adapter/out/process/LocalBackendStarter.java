package br.com.stowup.staff.adapter.out.process;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class LocalBackendStarter {
  private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(45);

  private LocalBackendStarter() {}

  public static CompletableFuture<Result> ensureRunning(String apiUrl) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return ensureRunningBlocking(apiUrl);
          } catch (IOException exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
          }
        });
  }

  static Result ensureRunningBlocking(String apiUrl) throws IOException {
    if (!isLocalApi(apiUrl) || !isWindows()) return Result.NOT_APPLICABLE;
    if (isReachable(apiUrl)) return Result.ALREADY_RUNNING;

    Path root = findProjectRoot(Path.of(System.getProperty("user.dir")));
    if (root == null) {
      throw new IOException(
          "Não encontrei tools/start-backend.ps1. Abra o projeto pela pasta raiz do StowUp.");
    }

    Path script = root.resolve("tools").resolve("start-backend.ps1");
    Process process =
        new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                script.toString())
            .directory(root.toFile())
            .inheritIO()
            .start();

    Instant deadline = Instant.now().plus(STARTUP_TIMEOUT);
    while (Instant.now().isBefore(deadline)) {
      if (isReachable(apiUrl)) return Result.STARTED;
      if (!process.isAlive()) {
        throw new IOException(
            "O inicializador local terminou antes de o Spring Boot ficar disponível.");
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IOException("A inicialização local foi interrompida.", exception);
      }
    }
    throw new IOException("O Spring Boot não ficou disponível dentro de 45 segundos.");
  }

  static boolean isLocalApi(String apiUrl) {
    try {
      String host = URI.create(apiUrl).getHost();
      return host != null
          && (host.equalsIgnoreCase("localhost")
              || host.equals("127.0.0.1")
              || host.equals("::1")
              || host.equals("[::1]"));
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
  }

  private static Path findProjectRoot(Path start) {
    Path current = start.toAbsolutePath().normalize();
    while (current != null) {
      if (Files.isRegularFile(current.resolve("tools").resolve("start-backend.ps1"))) {
        return current;
      }
      current = current.getParent();
    }
    return null;
  }

  private static boolean isReachable(String apiUrl) {
    try {
      String endpoint = apiUrl.replaceAll("/+$", "") + "/spaces";
      HttpURLConnection connection =
          (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
      connection.setConnectTimeout(800);
      connection.setReadTimeout(1_200);
      connection.setRequestMethod("GET");
      int status = connection.getResponseCode();
      connection.disconnect();
      return status == HttpURLConnection.HTTP_OK;
    } catch (IOException | IllegalArgumentException exception) {
      return false;
    }
  }

  public enum Result {
    NOT_APPLICABLE,
    ALREADY_RUNNING,
    STARTED
  }
}
