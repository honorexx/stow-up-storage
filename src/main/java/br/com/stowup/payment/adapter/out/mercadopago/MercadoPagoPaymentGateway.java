package br.com.stowup.payment.adapter.out.mercadopago;

import br.com.stowup.payment.application.port.out.PaymentGateway;
import br.com.stowup.payment.domain.*;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
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
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
  private final ObjectMapper json;

  public MercadoPagoPaymentGateway(
      @Value("${stowup.payments.mercado-pago.access-token:}") String accessToken,
      @Value("${stowup.public-url:http://localhost:8080}") String appUrl,
      ObjectMapper json) {
    this.accessToken = accessToken;
    this.appUrl = appUrl.replaceAll("/+$", "");
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
    payload.put("payer", Map.of("name", order.customerName(), "email", order.customerEmail()));
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
    JsonNode response = call("POST", "/checkout/preferences", payload);
    return new CheckoutSession(response.path("id").asText(), response.path("init_point").asText());
  }

  @Override
  public ProviderPayment findPayment(String providerPaymentId) {
    requireConfigured();
    JsonNode response = call("GET", "/v1/payments/" + providerPaymentId, null);
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
            "O Mercado Pago recusou a criação do checkout. Verifique as credenciais e os dados da reserva.");
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
}
