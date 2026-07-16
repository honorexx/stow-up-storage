package br.com.stowup.staff.adapter.out.process;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocalBackendStarterTest {
  @Test
  void recognizesOnlyLoopbackApisAsLocal() {
    assertTrue(LocalBackendStarter.isLocalApi("http://localhost:8080/api"));
    assertTrue(LocalBackendStarter.isLocalApi("http://127.0.0.1:8080/api"));
    assertTrue(LocalBackendStarter.isLocalApi("http://[::1]:8080/api"));
    assertFalse(LocalBackendStarter.isLocalApi("https://api.stowup.com.br/api"));
    assertFalse(LocalBackendStarter.isLocalApi("endereço inválido"));
  }
}
