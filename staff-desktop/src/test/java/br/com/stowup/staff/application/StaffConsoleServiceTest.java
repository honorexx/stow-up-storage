package br.com.stowup.staff.application;

import static org.junit.jupiter.api.Assertions.*;

import br.com.stowup.staff.application.port.out.StaffGateway;
import br.com.stowup.staff.domain.StaffSession;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class StaffConsoleServiceTest {
  @Test
  void logsInAnInternalUserAndNormalizesTheIdentifier() {
    AtomicReference<String> identifier = new AtomicReference<>();
    StaffGateway gateway =
        gateway(
            (method, args) -> {
              if (method.equals("login")) {
                identifier.set((String) args[0]);
                return new StaffSession("token", "Mestre", "master@stowup.local", "ADMIN");
              }
              return defaultValue(method);
            });

    StaffSession session =
        new StaffConsoleService(gateway).login("  master@stowup.local  ", "senha-segura");

    assertTrue(session.isMaster());
    assertEquals("master@stowup.local", identifier.get());
  }

  @Test
  void rejectsAnAccountWithoutInternalPermissions() {
    StaffGateway gateway =
        gateway(
            (method, args) ->
                method.equals("login")
                    ? new StaffSession("token", "Cliente", "cliente@test.local", "CUSTOMER")
                    : defaultValue(method));

    IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () -> new StaffConsoleService(gateway).login("cliente@test.local", "senha-segura"));

    assertTrue(error.getMessage().contains("não possui acesso"));
  }

  @Test
  void enforcesRoleBoundariesBeforeCallingTheGateway() {
    StaffConsoleService service = new StaffConsoleService(gateway((method, args) -> List.of()));
    StaffSession support = new StaffSession("token", "Suporte", "suporte@test.local", "SUPPORT");

    assertDoesNotThrow(() -> service.tickets(support, "OPEN"));
    assertThrows(IllegalArgumentException.class, () -> service.pendingSpaces(support));
    assertThrows(IllegalArgumentException.class, () -> service.staffUsers(support));
  }

  @Test
  void validatesStaffDataAtTheApplicationBoundary() {
    StaffConsoleService service = new StaffConsoleService(gateway((method, args) -> null));
    StaffSession master = new StaffSession("token", "Mestre", "master@test.local", "ADMIN");

    assertThrows(
        IllegalArgumentException.class,
        () -> service.createStaff(master, "A", "invalido", "curta", "SUPPORT"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.createStaff(
                master, "Pessoa Teste", "pessoa@test.local", "senha-segura", "CUSTOMER"));
  }

  private static StaffGateway gateway(Invocation invocation) {
    return (StaffGateway)
        Proxy.newProxyInstance(
            StaffGateway.class.getClassLoader(),
            new Class<?>[] {StaffGateway.class},
            (proxy, method, args) -> invocation.call(method.getName(), args));
  }

  private static Object defaultValue(String method) {
    return method.equals("tickets") ? List.of() : null;
  }

  @FunctionalInterface
  private interface Invocation {
    Object call(String method, Object[] args);
  }
}
