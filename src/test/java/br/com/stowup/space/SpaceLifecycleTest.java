package br.com.stowup.space;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.stowup.user.Role;
import br.com.stowup.user.UserAccount;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpaceLifecycleTest {
  @Test
  void tracksCapacityAndTemporarySuspension() {
    UserAccount owner = new UserAccount("Proprietário", "capacity@stowup.test", "hash", Role.OWNER);
    Space space =
        new Space(
            "Galpão",
            "Curitiba",
            "Boqueirão",
            "Galpão",
            100,
            new BigDecimal("1800.00"),
            0,
            0,
            0,
            true,
            "Galpão para estoque",
            List.of("Acesso para caminhões"),
            "#002B5B",
            "#FF5A1F",
            owner);

    space.setCapacityTotal(40);
    space.occupy(15);
    assertThat(space.getCapacityAvailable()).isEqualTo(25);
    assertThat(space.getLifecycleStatus()).isEqualTo("ACTIVE");

    space.suspendTemporarily();
    assertThat(space.isAvailable()).isFalse();
    assertThat(space.getLifecycleStatus()).isEqualTo("TEMP_UNAVAILABLE");

    space.reactivate();
    assertThat(space.isAvailable()).isTrue();
    assertThat(space.getLifecycleStatus()).isEqualTo("ACTIVE");
  }
}
