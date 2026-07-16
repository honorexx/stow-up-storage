package br.com.stowup.payment.adapter.out.mercadopago;

import br.com.stowup.payment.application.port.out.PaymentGateway;
import br.com.stowup.payment.domain.*;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MercadoPagoPaymentGateway implements PaymentGateway {
  private final String accessToken;
  private final String appUrl;
  private final boolean testMode;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
  private final ObjectMapper json;

  public MercadoPagoPaymentGateway(
      @Value("${stowup.payments.mercado-pago.access-token:}") String accessToken,
      @Value("${stowup.public-url:http://localhost:8080}") String appUrl,
      @Value("${stowup.payments.mercado-pago.test-mode:false}") boolean testMode,
      ObjectMapper json) {
    this.accessToken = accessToken;
    this.appUrl = appUrl.replaceAll("/+$", "");
    this.testMode = testMode;
    this.json = json;
  }

  @Override
  public CheckoutSession createCheckout(PaymentOrder order) {
    requireConfigured();
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("external_reference", order.bookingId());
    payload.put(
        "items",
        List.of(
            Map.of(
                "id",
                order.bookingId(),
                "title",
                order.title(),
                "quantity",
                1,
                "currency_id",
                "BRL",
                "unit_price",
                order.amount())));
    if (!testMode)
      payload.put("payer", Map.of("name", order.customerName(), "email", order.customerEmail()));
    if (!testMode) {
      requirePublicHttpsUrl();
      payload.put(
          "back_urls",
          Map.of(
              "success",
              appUrl + "/pagamento/sucesso",
              "pending",
              appUrl + "/pagamento/pendente",
              "failure",
              appUrl + "/pagamento/falha"));
      payload.put("auto_return", "approved");
      payload.put("notification_url", appUrl + "/api/payments/webhooks/mercado-pago");
    }
    JsonNode response = call("POST", "/checkout/preferences", payload);
    return new CheckoutSession(response.path("id").asText(), selectCheckoutUrl(response, testMode));
  }

  static String selectCheckoutUrl(JsonNode response, boolean testMode) {
    String field = testMode ? "sandbox_init_point" : "init_point";
    String checkoutUrl = response.path(field).asText();
    if (checkoutUrl.isBlank())
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY,
          "O Mercado Pago não retornou a URL do checkout "
              + (testMode ? "de teste." : "de produção."));
    return checkoutUrl;
  }

  @Override
  public ProviderPayment findPayment(String providerPaymentId) {
    requireConfigured();
    JsonNode response = call("GET", "/v1/payments/" + providerPaymentId, null);
    return toProviderPayment(response);
  }

  @Override
  public Optional<ProviderPayment> findLatestPayment(String externalReference) {
    requireConfigured();
    String encodedReference = URLEncoder.encode(externalReference, StandardCharsets.UTF_8);
    JsonNode response =
        call(
            "GET",
            "/v1/payments/search?external_reference="
                + encodedReference
                + "&sort=date_created&criteria=desc",
            null);
    JsonNode results = response.path("results");
    if (!results.isArray() || results.isEmpty()) return Optional.empty();
    return Optional.of(toProviderPayment(results.get(0)));
  }

  @Override
  public ProviderPayment refundPayment(String providerPaymentId) {
    requireConfigured();
    call("POST", "/v1/payments/" + providerPaymentId + "/refunds", Map.of());
    return findPayment(providerPaymentId);
  }

  private ProviderPayment toProviderPayment(JsonNode response) {
    return new ProviderPayment(
        response.path("id").asText(),
        response.path("external_reference").asText(),
        response.path("status").asText(),
        response.path("transaction_amount").decimalValue(),
        response.path("currency_id").asText());
  }

  private JsonNode call(String method, String path, Object body) {
    try {
      HttpRequest.Builder request =
          HttpRequest.newBuilder(URI.create("https://api.mercadopago.com" + path))
              .timeout(Duration.ofSeconds(20))
              .header("Authorization", "Bearer " + accessToken)
              .header("Accept", "application/json");
      if (body == null) request.GET();
      else
        request
            .header("Content-Type", "application/json")
            .method(
                method,
                HttpRequest.BodyPublishers.ofString(
                    json.writeValueAsString(body), StandardCharsets.UTF_8));
      HttpResponse<String> response =
          http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() / 100 != 2)
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "O Mercado Pago recusou o checkout: " + providerMessage(response.body()));
      return json.readTree(response.body());
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Não foi possível conectar ao Mercado Pago.", e);
    }
  }

  private void requireConfigured() {
    if (accessToken == null || accessToken.isBlank())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Configure MERCADO_PAGO_ACCESS_TOKEN para ativar pagamentos reais ou de teste.");
  }

  private void requirePublicHttpsUrl() {
    URI publicUri;
    try {
      publicUri = URI.create(appUrl);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Configure uma URL HTTPS pública para os pagamentos.");
    }
    String host = publicUri.getHost();
    if (!"https".equalsIgnoreCase(publicUri.getScheme())
        || host == null
        || host.equalsIgnoreCase("localhost")
        || host.startsWith("127.")) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Configure STOWUP_PUBLIC_URL com uma URL HTTPS pública antes de ativar pagamentos reais.");
    }
  }

  String providerMessage(String responseBody) {
    try {
      String message = json.readTree(responseBody).path("message").asText();
      return message.isBlank() ? "verifique os dados enviados." : message;
    } catch (Exception ignored) {
      return "verifique os dados enviados.";
    }
  }
}
