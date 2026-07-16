package br.com.stowup.staff.adapter.out.http;

import br.com.stowup.staff.application.port.out.StaffGateway;
import br.com.stowup.staff.domain.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HttpStaffGateway implements StaffGateway {
  private final String apiUrl;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
  private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

  public HttpStaffGateway(String apiUrl) {
    this.apiUrl = apiUrl.replaceAll("/+$", "");
  }

  @Override
  public StaffSession login(String email, String password) {
    JsonNode body =
        read(
            request("/auth/login", null, "POST", Map.of("email", email, "password", password)),
            JsonNode.class);
    return new StaffSession(
        body.path("token").asText(),
        body.path("user").path("name").asText(),
        body.path("user").path("email").asText(),
        body.path("user").path("role").asText());
  }

  @Override
  public AdminDashboard dashboard(StaffSession session) {
    return read(request("/admin/dashboard", session, "GET", null), AdminDashboard.class);
  }

  @Override
  public List<AppUser> appUsers(StaffSession session) {
    return read(request("/admin/users", session, "GET", null), new TypeReference<>() {});
  }

  @Override
  public AppUser updateAppUser(
      StaffSession session,
      long id,
      String name,
      String phone,
      String city,
      String state,
      String role,
      boolean enabled,
      boolean ownerVerified) {
    return read(
        request(
            "/admin/users/" + id,
            session,
            "PATCH",
            Map.of(
                "name", name,
                "phone", phone,
                "city", city,
                "state", state,
                "role", role,
                "enabled", enabled,
                "ownerVerified", ownerVerified)),
        AppUser.class);
  }

  @Override
  public void deleteAppUser(StaffSession session, long id) {
    request("/admin/users/" + id, session, "DELETE", null);
  }

  @Override
  public List<ManagedSpace> managedSpaces(StaffSession session) {
    return read(request("/admin/spaces", session, "GET", null), new TypeReference<>() {});
  }

  @Override
  public ManagedSpace updateManagedSpace(StaffSession session, ManagedSpace space) {
    return read(
        request(
            "/admin/spaces/" + space.id(),
            session,
            "PATCH",
            Map.ofEntries(
                Map.entry("title", space.title()),
                Map.entry("description", space.description()),
                Map.entry("type", space.type()),
                Map.entry("heightMeters", space.heightMeters()),
                Map.entry("lengthMeters", space.lengthMeters()),
                Map.entry("depthMeters", space.depthMeters()),
                Map.entry("price", space.price()),
                Map.entry("city", space.city()),
                Map.entry("district", space.district()),
                Map.entry("postalCode", space.postalCode()),
                Map.entry("addressLine", space.addressLine()),
                Map.entry("addressNumber", space.addressNumber()),
                Map.entry(
                    "addressComplement",
                    space.addressComplement() == null ? "" : space.addressComplement()),
                Map.entry("state", space.state()))),
        ManagedSpace.class);
  }

  @Override
  public ManagedSpace uploadManagedSpacePhoto(StaffSession session, long id, Path file) {
    return read(multipart("/admin/spaces/" + id + "/photos", session, file), ManagedSpace.class);
  }

  @Override
  public void withdrawManagedSpace(StaffSession session, long id) {
    request("/admin/spaces/" + id, session, "DELETE", null);
  }

  @Override
  public List<AdminSpaceDocument> spaceDocuments(StaffSession session, long spaceId) {
    return read(
        request("/admin/spaces/" + spaceId + "/documents", session, "GET", null),
        new TypeReference<>() {});
  }

  @Override
  public byte[] downloadSpaceDocument(StaffSession session, String documentId) {
    return requestBytes("/admin/space-documents/" + documentId + "/download", session);
  }

  @Override
  public List<SupportTicket> tickets(StaffSession session, String status) {
    return read(
        request("/staff/support/conversations?status=" + status, session, "GET", null),
        new TypeReference<>() {});
  }

  @Override
  public List<SupportMessage> messages(StaffSession session, String ticketId) {
    return read(
        request("/staff/support/conversations/" + ticketId + "/messages", session, "GET", null),
        new TypeReference<>() {});
  }

  @Override
  public SupportMessage reply(StaffSession session, String ticketId, String body) {
    return read(
        request(
            "/staff/support/conversations/" + ticketId + "/messages",
            session,
            "POST",
            Map.of("body", body)),
        SupportMessage.class);
  }

  @Override
  public SupportMessage uploadSupportAttachment(StaffSession session, String ticketId, Path file) {
    return read(
        multipart("/staff/support/conversations/" + ticketId + "/attachments", session, file),
        SupportMessage.class);
  }

  @Override
  public byte[] downloadSupportAttachment(StaffSession session, String ticketId, long messageId) {
    return requestBytes(
        "/staff/support/conversations/" + ticketId + "/messages/" + messageId + "/attachment",
        session);
  }

  @Override
  public void changeTicketStatus(StaffSession session, String ticketId, String status) {
    request(
        "/staff/support/conversations/" + ticketId + "/status",
        session,
        "PATCH",
        Map.of("status", status));
  }

  @Override
  public List<PendingSpace> pendingSpaces(StaffSession session) {
    return read(
        request("/staff/moderation/spaces/pending", session, "GET", null),
        new TypeReference<>() {});
  }

  @Override
  public void approveSpace(StaffSession session, long spaceId) {
    request("/staff/moderation/spaces/" + spaceId + "/approve", session, "POST", Map.of());
  }

  @Override
  public void rejectSpace(StaffSession session, long spaceId, String reason) {
    request(
        "/staff/moderation/spaces/" + spaceId + "/reject",
        session,
        "POST",
        Map.of("reason", reason));
  }

  @Override
  public List<StaffContract> contracts(StaffSession session, String status) {
    return read(
        request("/staff/contracts?status=" + status, session, "GET", null),
        new TypeReference<>() {});
  }

  @Override
  public byte[] signedContract(StaffSession session, String contractId) {
    return requestBytes("/staff/contracts/" + contractId + "/signed", session);
  }

  @Override
  public void reviewContract(
      StaffSession session,
      String contractId,
      String result,
      boolean signerNameMatches,
      String validatorReference,
      String notes) {
    request(
        "/staff/contracts/" + contractId + "/review",
        session,
        "POST",
        Map.of(
            "result",
            result,
            "signerNameMatches",
            signerNameMatches,
            "validatorReportReference",
            validatorReference,
            "notes",
            notes));
  }

  @Override
  public List<StaffUser> staffUsers(StaffSession session) {
    return read(request("/admin/staff", session, "GET", null), new TypeReference<>() {});
  }

  @Override
  public StaffUser createStaff(
      StaffSession session, String name, String email, String password, String role) {
    return read(
        request(
            "/admin/staff",
            session,
            "POST",
            Map.of("name", name, "email", email, "password", password, "role", role)),
        StaffUser.class);
  }

  @Override
  public StaffUser updateStaff(
      StaffSession session, long id, String name, String role, boolean enabled) {
    return read(
        request(
            "/admin/staff/" + id,
            session,
            "PATCH",
            Map.of("name", name, "role", role, "enabled", enabled)),
        StaffUser.class);
  }

  @Override
  public void resetStaffPassword(StaffSession session, long id, String password) {
    request(
        "/admin/staff/" + id + "/reset-password", session, "POST", Map.of("password", password));
  }

  @Override
  public void deleteStaff(StaffSession session, long id) {
    request("/admin/staff/" + id, session, "DELETE", null);
  }

  @Override
  public List<ListingDocument> listingDocuments(StaffSession session) {
    return read(
        request("/staff/listing-documents", session, "GET", null), new TypeReference<>() {});
  }

  @Override
  public byte[] signedListingDocument(StaffSession session, String id) {
    return requestBytes("/staff/listing-documents/" + id + "/signed", session);
  }

  @Override
  public void reviewListingDocument(
      StaffSession session,
      String id,
      boolean approved,
      boolean signerNameMatches,
      String validatorReference,
      String notes) {
    request(
        "/staff/listing-documents/" + id + "/review",
        session,
        "POST",
        Map.of(
            "approved",
            approved,
            "signerNameMatches",
            signerNameMatches,
            "validatorReference",
            validatorReference,
            "notes",
            notes));
  }

  private String request(String path, StaffSession session, String method, Object payload) {
    try {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder(URI.create(apiUrl + path))
              .timeout(Duration.ofSeconds(15))
              .header("Accept", "application/json");
      if (session != null) builder.header("Authorization", "Bearer " + session.token());
      if (payload != null)
        builder
            .header("Content-Type", "application/json")
            .method(
                method,
                HttpRequest.BodyPublishers.ofString(
                    json.writeValueAsString(payload), StandardCharsets.UTF_8));
      else builder.method(method, HttpRequest.BodyPublishers.noBody());
      HttpResponse<String> response =
          http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() / 100 != 2) {
        JsonNode error = response.body().isBlank() ? null : json.readTree(response.body());
        throw new IllegalStateException(
            error == null
                ? "Falha na comunicação com o StowUp (" + response.statusCode() + ")."
                : error.path("message").asText("A operação não pôde ser concluída."));
      }
      return response.body();
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(
          "Não foi possível conectar ao backend em "
              + apiUrl
              + ". Verifique se o Spring Boot está iniciado.",
          e);
    }
  }

  private String multipart(String path, StaffSession session, Path file) {
    try {
      String boundary = "StowUp-" + UUID.randomUUID();
      String contentType = Files.probeContentType(file);
      if (contentType == null) contentType = "application/octet-stream";
      byte[] prefix =
          ("--"
                  + boundary
                  + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                  + file.getFileName().toString().replace("\"", "")
                  + "\"\r\nContent-Type: "
                  + contentType
                  + "\r\n\r\n")
              .getBytes(StandardCharsets.UTF_8);
      byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(apiUrl + path))
              .timeout(Duration.ofSeconds(45))
              .header("Authorization", "Bearer " + session.token())
              .header("Accept", "application/json")
              .header("Content-Type", "multipart/form-data; boundary=" + boundary)
              .POST(
                  HttpRequest.BodyPublishers.concat(
                      HttpRequest.BodyPublishers.ofByteArray(prefix),
                      HttpRequest.BodyPublishers.ofFile(file),
                      HttpRequest.BodyPublishers.ofByteArray(suffix)))
              .build();
      HttpResponse<String> response =
          http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() / 100 != 2) {
        JsonNode error = response.body().isBlank() ? null : json.readTree(response.body());
        throw new IllegalStateException(
            error == null
                ? "Falha ao enviar o arquivo (" + response.statusCode() + ")."
                : error.path("message").asText("Não foi possível enviar o arquivo."));
      }
      return response.body();
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Não foi possível enviar o arquivo.", e);
    }
  }

  private byte[] requestBytes(String path, StaffSession session) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(apiUrl + path))
              .timeout(Duration.ofSeconds(20))
              .header("Authorization", "Bearer " + session.token())
              .GET()
              .build();
      HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() / 100 != 2)
        throw new IllegalStateException("Não foi possível baixar o PDF assinado.");
      return response.body();
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Não foi possível baixar o PDF assinado.", e);
    }
  }

  private <T> T read(String value, Class<T> type) {
    try {
      return json.readValue(value, type);
    } catch (Exception e) {
      throw new IllegalStateException("Resposta inválida do servidor.", e);
    }
  }

  private <T> T read(String value, TypeReference<T> type) {
    try {
      return json.readValue(value, type);
    } catch (Exception e) {
      throw new IllegalStateException("Resposta inválida do servidor.", e);
    }
  }
}
