package br.com.stowup.staff.adapter.out.http;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HttpStaffGatewayTest {
  @Test
  void explainsWhenTheBackendIsUnavailable() {
    HttpStaffGateway gateway = new HttpStaffGateway("http://127.0.0.1:1/api");

    IllegalStateException error =
        assertThrows(
            IllegalStateException.class,
            () -> gateway.login("master@stowup.local", "senha-inexistente"));

    assertTrue(error.getMessage().contains("Verifique se o Spring Boot está iniciado"));
  }
}
