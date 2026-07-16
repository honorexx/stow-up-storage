package br.com.stowup.payment.adapter.out.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MercadoPagoPaymentGatewayTest {
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void selectsSandboxCheckoutUrlInTestMode() throws Exception {
    var response =
        json.readTree(
            """
            {
              "init_point": "https://www.mercadopago.com.br/checkout/v1/redirect",
              "sandbox_init_point": "https://sandbox.mercadopago.com.br/checkout/v1/redirect"
            }
            """);

    assertThat(MercadoPagoPaymentGateway.selectCheckoutUrl(response, true))
        .startsWith("https://sandbox.mercadopago.com.br/");
  }

  @Test
  void selectsProductionCheckoutUrlOutsideTestMode() throws Exception {
    var response =
        json.readTree(
            """
            {
              "init_point": "https://www.mercadopago.com.br/checkout/v1/redirect",
              "sandbox_init_point": "https://sandbox.mercadopago.com.br/checkout/v1/redirect"
            }
            """);

    assertThat(MercadoPagoPaymentGateway.selectCheckoutUrl(response, false))
        .startsWith("https://www.mercadopago.com.br/");
  }

  @Test
  void extractsProviderMessageWithoutExposingTheWholeResponse() {
    var gateway =
        new MercadoPagoPaymentGateway(
            "APP_USR-test", "http://localhost:8080", true, new ObjectMapper());

    assertThat(
            gateway.providerMessage(
                """
                {"message":"auto_return invalid. back_url.success must be defined","error":"invalid_auto_return"}
                """))
        .isEqualTo("auto_return invalid. back_url.success must be defined");
  }
}
