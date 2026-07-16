package br.com.stowup.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MasterAccountBootstrapTest {
  @Test
  void repairsNameWrittenByLegacyWindowsEncoding() {
    assertEquals("Usuário Mestre", MasterAccountBootstrap.repairLegacyEncoding("UsuÃ¡rio Mestre"));
  }

  @Test
  void keepsCorrectPortugueseTextUnchanged() {
    assertEquals("Usuário Mestre", MasterAccountBootstrap.repairLegacyEncoding("Usuário Mestre"));
    assertEquals("SÃO PAULO", MasterAccountBootstrap.repairLegacyEncoding("SÃO PAULO"));
  }
}
