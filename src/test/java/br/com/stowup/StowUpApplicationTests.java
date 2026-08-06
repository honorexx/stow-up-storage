package br.com.stowup;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.stowup.user.Role;
import br.com.stowup.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StowUpApplicationTests {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository users;

  @Test
  void doesNotExposePrivateUploadedDocumentsAsStaticFiles() throws Exception {
    mvc.perform(get("/staff/")).andExpect(status().isOk());
    mvc.perform(get("/uploads/contracts/signed-contract.pdf")).andExpect(status().isForbidden());
    mvc.perform(get("/uploads/listing-documents/signed/document.pdf"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/uploads/space-documents/1/license.pdf")).andExpect(status().isForbidden());
    mvc.perform(get("/uploads/profiles/missing.png")).andExpect(status().isNotFound());
  }

  @Test
  void registersAuthenticatesAndUpdatesARealAccountWithoutSeededSpaces() throws Exception {
    mvc.perform(get("/api/spaces")).andExpect(status().isOk()).andExpect(content().json("[]"));

    String registerBody =
        mvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Cliente Teste\",\"email\":\"cliente@stowup.test\",\"password\":\"Senha@123\",\"phone\":\"+55 11 99999-0000\",\"cpf\":\"529.982.247-25\",\"postalCode\":\"01310-100\",\"addressLine\":\"Avenida Paulista\",\"addressNumber\":\"1000\",\"addressComplement\":\"Apto 10\",\"district\":\"Bela Vista\",\"city\":\"São Paulo\",\"state\":\"SP\",\"acceptedTerms\":true,\"termsVersion\":\"2026-07-13\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.user.name").value("Cliente Teste"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode register = json.readTree(registerBody);

    mvc.perform(
            post("/api/auth/staff-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"cliente@stowup.test\",\"password\":\"Senha@123\"}"))
        .andExpect(status().isUnauthorized());

    mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Outro Cliente\",\"email\":\"cliente@stowup.test\",\"password\":\"Senha@123\",\"phone\":\"+55 11 97777-0000\",\"cpf\":\"390.533.447-05\",\"postalCode\":\"01310-100\",\"addressLine\":\"Avenida Paulista\",\"addressNumber\":\"10\",\"addressComplement\":\"\",\"district\":\"Bela Vista\",\"city\":\"São Paulo\",\"state\":\"SP\",\"acceptedTerms\":true,\"termsVersion\":\"2026-07-13\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Este e-mail já está cadastrado."));

    mvc.perform(
            patch("/api/profile")
                .header("Authorization", "Bearer " + register.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Cliente Atualizado\",\"phone\":\"+55 11 99999-0000\",\"postalCode\":\"01310-100\",\"addressLine\":\"Avenida Paulista\",\"addressNumber\":\"1001\",\"addressComplement\":\"\",\"district\":\"Bela Vista\",\"city\":\"São Paulo\",\"state\":\"SP\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Cliente Atualizado"))
        .andExpect(jsonPath("$.phone").value("+55 11 99999-0000"));

    mvc.perform(
            get("/api/owner/spaces")
                .header("Authorization", "Bearer " + register.get("token").asText()))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    mvc.perform(
            post("/api/owner/spaces")
                .header("Authorization", "Bearer " + register.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Box seco próximo ao metrô\",\"city\":\"São Paulo\",\"district\":\"Bela Vista\",\"postalCode\":\"01310-100\",\"addressLine\":\"Avenida Paulista\",\"addressNumber\":\"1001\",\"addressComplement\":\"Subsolo\",\"state\":\"SP\",\"type\":\"Box\",\"size\":8,\"heightMeters\":2.5,\"lengthMeters\":4,\"depthMeters\":2,\"price\":350.00,\"latitude\":-23.5614,\"longitude\":-46.6559,\"description\":\"Espaço seco, monitorado e com acesso controlado todos os dias.\",\"features\":[\"Câmeras\",\"Acesso controlado\"]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.verified").value(false))
        .andExpect(jsonPath("$.photos").isArray())
        .andExpect(jsonPath("$.addressLine").value("Avenida Paulista"));

    byte[] twoMegabyteImage = new byte[2 * 1024 * 1024];
    twoMegabyteImage[0] = (byte) 0xff;
    twoMegabyteImage[1] = (byte) 0xd8;
    twoMegabyteImage[2] = (byte) 0xff;
    MockMultipartFile photo =
        new MockMultipartFile("file", "espaco.jpg", "image/jpeg", twoMegabyteImage);
    mvc.perform(
            multipart("/api/owner/spaces/1/photos")
                .file(photo)
                .header("Authorization", "Bearer " + register.get("token").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.photos.length()").value(1));

    MockMultipartFile fakePhoto =
        new MockMultipartFile("file", "nao-e-foto.jpg", "image/jpeg", "conteudo falso".getBytes());
    mvc.perform(
            multipart("/api/owner/spaces/1/photos")
                .file(fakePhoto)
                .header("Authorization", "Bearer " + register.get("token").asText()))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("O conteúdo do arquivo não corresponde a uma imagem válida."));

    mvc.perform(
            get("/api/owner/spaces")
                .header("Authorization", "Bearer " + register.get("token").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Box seco próximo ao metrô"));

    mvc.perform(get("/api/spaces")).andExpect(status().isOk()).andExpect(content().json("[]"));

    String renterBody =
        mvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Locatário Teste\",\"email\":\"locatario@stowup.test\",\"password\":\"Senha@123\",\"phone\":\"+55 (11) 98888-0000\",\"cpf\":\"111.444.777-35\",\"postalCode\":\"01001-000\",\"addressLine\":\"Praça da Sé\",\"addressNumber\":\"10\",\"addressComplement\":\"\",\"district\":\"Sé\",\"city\":\"São Paulo\",\"state\":\"SP\",\"acceptedTerms\":true,\"termsVersion\":\"2026-07-13\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode renter = json.readTree(renterBody);
    String conversationBody =
        mvc.perform(
                post("/api/conversations")
                    .header("Authorization", "Bearer " + renter.get("token").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"spaceId\":1}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.participantName").value("Cliente Atualizado"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String conversationId = json.readTree(conversationBody).get("id").asText();
    mvc.perform(
            post("/api/conversations/" + conversationId + "/messages")
                .header("Authorization", "Bearer " + renter.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Olá, o espaço ainda está disponível?\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.mine").value(true));
    mvc.perform(
            get("/api/conversations")
                .header("Authorization", "Bearer " + register.get("token").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].lastMessage").value("Olá, o espaço ainda está disponível?"));

    String supportTicketBody =
        mvc.perform(
                post("/api/support/tickets")
                    .header("Authorization", "Bearer " + register.get("token").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"category\":\"LISTING\",\"subject\":\"Ajuda com anúncio\",\"description\":\"Preciso de ajuda com meu anúncio.\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String supportTicketId = json.readTree(supportTicketBody).get("id").asText();
    mvc.perform(
            get("/api/staff/support/conversations")
                .header("Authorization", "Bearer " + register.get("token").asText()))
        .andExpect(status().isForbidden());

    String staffBody =
        mvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Atendente Teste\",\"email\":\"suporte@stowup.test\",\"password\":\"Senha@123\",\"phone\":\"+55 (11) 96666-0000\",\"cpf\":\"123.456.789-09\",\"postalCode\":\"01001-000\",\"addressLine\":\"Praça da Sé\",\"addressNumber\":\"20\",\"addressComplement\":\"\",\"district\":\"Sé\",\"city\":\"São Paulo\",\"state\":\"SP\",\"acceptedTerms\":true,\"termsVersion\":\"2026-07-13\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode staff = json.readTree(staffBody);
    var staffAccount = users.findByEmailIgnoreCase("suporte@stowup.test").orElseThrow();
    staffAccount.setRole(Role.SUPPORT);
    users.saveAndFlush(staffAccount);
    String queueBody =
        mvc.perform(
                get("/api/staff/support/conversations")
                    .header("Authorization", "Bearer " + staff.get("token").asText()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].customerEmail").value("cliente@stowup.test"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String supportConversationId = json.readTree(queueBody).get(0).get("id").asText();
    mvc.perform(
            post("/api/staff/support/conversations/" + supportConversationId + "/messages")
                .header("Authorization", "Bearer " + staff.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Olá! Já estamos verificando o seu anúncio.\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.senderType").value("SUPPORT"));
    mvc.perform(
            get("/api/support/tickets/" + supportTicketId + "/messages")
                .header("Authorization", "Bearer " + register.get("token").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[1].senderType").value("SUPPORT"));

    String masterBody =
        mvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Gestor Mestre\",\"email\":\"mestre@stowup.test\",\"password\":\"SenhaMestre@123\",\"phone\":\"+55 (11) 95555-0000\",\"cpf\":\"987.654.321-00\",\"postalCode\":\"01001-000\",\"addressLine\":\"Praça da Sé\",\"addressNumber\":\"30\",\"addressComplement\":\"\",\"district\":\"Sé\",\"city\":\"São Paulo\",\"state\":\"SP\",\"acceptedTerms\":true,\"termsVersion\":\"2026-07-13\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode master = json.readTree(masterBody);
    var masterAccount = users.findByEmailIgnoreCase("mestre@stowup.test").orElseThrow();
    masterAccount.setRole(Role.ADMIN);
    users.saveAndFlush(masterAccount);

    String createdStaffBody =
        mvc.perform(
                post("/api/admin/staff")
                    .header("Authorization", "Bearer " + master.get("token").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Analista Teste\",\"email\":\"analista@stowup.test\",\"password\":\"Temporaria@123\",\"role\":\"LISTING_ANALYST\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("LISTING_ANALYST"))
            .andExpect(
                jsonPath("$.staffCode").value(org.hamcrest.Matchers.matchesPattern("SU\\d{6}")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String staffCode = json.readTree(createdStaffBody).get("staffCode").asText();

    String moderatorBody =
        mvc.perform(
                post("/api/auth/staff-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + staffCode + "\",\"password\":\"Temporaria@123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String moderatorToken = json.readTree(moderatorBody).get("token").asText();
    mvc.perform(
            get("/api/staff/moderation/spaces/pending")
                .header("Authorization", "Bearer " + moderatorToken))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/staff/support/conversations")
                .header("Authorization", "Bearer " + moderatorToken))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/staff").header("Authorization", "Bearer " + moderatorToken))
        .andExpect(status().isForbidden());

    byte[] validPng = new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
    for (int index = 2; index <= 4; index++) {
      mvc.perform(
              multipart("/api/owner/spaces/1/photos")
                  .file(
                      new MockMultipartFile(
                          "file", "espaco-" + index + ".png", "image/png", validPng))
                  .header("Authorization", "Bearer " + register.get("token").asText()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.photos.length()").value(index));
    }

    mvc.perform(
            patch("/api/owner/spaces/1")
                .header("Authorization", "Bearer " + register.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Box atualizado pelo proprietário\",\"city\":\"São Paulo\",\"district\":\"Bela Vista\",\"postalCode\":\"01310-100\",\"addressLine\":\"Avenida Paulista\",\"addressNumber\":\"1001\",\"addressComplement\":\"Subsolo\",\"state\":\"SP\",\"type\":\"Box\",\"size\":8,\"heightMeters\":2.5,\"lengthMeters\":4,\"depthMeters\":2,\"price\":365.00,\"latitude\":-23.5614,\"longitude\":-46.6559,\"description\":\"Espaço atualizado, seco, monitorado e com acesso controlado todos os dias.\",\"features\":[\"Câmeras\",\"Acesso controlado\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Box atualizado pelo proprietário"))
        .andExpect(jsonPath("$.price").value(365.00))
        .andExpect(jsonPath("$.moderationStatus").value("PENDING"))
        .andExpect(jsonPath("$.lifecycleStatus").value("IN_REVIEW"))
        .andExpect(jsonPath("$.photos.length()").value(4));

    String listingDocumentBody =
        mvc.perform(
                get("/api/owner/spaces/1/document")
                    .header("Authorization", "Bearer " + register.get("token").asText()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("GENERATED"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String listingDocumentId = json.readTree(listingDocumentBody).get("id").asText();
    mvc.perform(
            multipart("/api/owner/spaces/1/document/signed")
                .file(
                    new MockMultipartFile(
                        "file",
                        "termo-assinado.pdf",
                        "application/pdf",
                        "%PDF-1.4\n%%EOF".getBytes()))
                .header("Authorization", "Bearer " + register.get("token").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

    mvc.perform(
            post("/api/staff/listing-documents/" + listingDocumentId + "/review")
                .header("Authorization", "Bearer " + master.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"approved\":true,\"signerNameMatches\":true,\"validatorReference\":\"VALIDAR-ITI-QA\",\"notes\":\"Assinatura e titularidade conferidas.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));

    mvc.perform(
            patch("/api/admin/spaces/1")
                .header("Authorization", "Bearer " + master.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Box revisado próximo ao metrô\",\"description\":\"Espaço seco, monitorado e com acesso controlado todos os dias.\",\"type\":\"Box\",\"heightMeters\":2.5,\"lengthMeters\":4,\"depthMeters\":2,\"price\":375.00,\"city\":\"São Paulo\",\"district\":\"Bela Vista\",\"postalCode\":\"01310-100\",\"addressLine\":\"Avenida Paulista\",\"addressNumber\":\"1001\",\"addressComplement\":\"Subsolo\",\"state\":\"SP\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Box revisado próximo ao metrô"))
        .andExpect(jsonPath("$.photos.length()").value(4));

    mvc.perform(
            post("/api/admin/spaces/1/approve")
                .header("Authorization", "Bearer " + master.get("token").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.moderationStatus").value("APPROVED"));

    mvc.perform(get("/api/spaces/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Box revisado próximo ao metrô"))
        .andExpect(jsonPath("$.photos.length()").value(4));
    mvc.perform(get("/api/spaces?query=metrô"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));

    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"cliente@stowup.test\",\"password\":\"Senha@123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.name").value("Cliente Atualizado"))
        .andExpect(jsonPath("$.user.role").value("OWNER"))
        .andExpect(jsonPath("$.user.cpfMasked").value("***.***.***-25"));
  }
}
