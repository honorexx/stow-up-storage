package br.com.stowup.booking;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.stowup.space.Space;
import br.com.stowup.user.Role;
import br.com.stowup.user.UserAccount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookingLifecycleTest {
  @Test
  void calculatesEndDateAndCompletesWhenCustomerChecksOut() {
    UserAccount owner = new UserAccount("Proprietário", "owner@stowup.test", "hash", Role.OWNER);
    UserAccount customer =
        new UserAccount("Cliente", "customer@stowup.test", "hash", Role.CUSTOMER);
    Space space =
        new Space(
            "Box",
            "Curitiba",
            "Centro",
            "Box",
            12,
            new BigDecimal("350.00"),
            0,
            0,
            0,
            true,
            "Espaço seguro para armazenamento",
            List.of("Câmeras"),
            "#002B5B",
            "#FF5A1F",
            owner);
    Booking booking =
        new Booking(
            "RES-TESTE",
            space,
            customer,
            LocalDate.of(2026, 7, 20),
            3,
            new BigDecimal("1050.00"),
            BookingStatus.CONFIRMED,
            "CTR-TESTE",
            "PIX");

    assertThat(booking.getEndDate()).isEqualTo(LocalDate.of(2026, 10, 20));
    assertThat(booking.confirmCheckout(true, LocalDate.of(2026, 10, 20))).isTrue();
    assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    assertThat(booking.getCompletedAt()).isNotNull();
  }

  @Test
  void ownerConfirmationAloneDoesNotCompleteRental() {
    UserAccount owner = new UserAccount("Proprietário", "owner2@stowup.test", "hash", Role.OWNER);
    UserAccount customer =
        new UserAccount("Cliente", "customer2@stowup.test", "hash", Role.CUSTOMER);
    Space space =
        new Space(
            "Garagem",
            "Curitiba",
            "Água Verde",
            "Garagem",
            20,
            new BigDecimal("500.00"),
            0,
            0,
            0,
            true,
            "Garagem coberta",
            List.of("Acesso 24 horas"),
            "#002B5B",
            "#FF5A1F",
            owner);
    Booking booking =
        new Booking(
            "RES-TESTE2",
            space,
            customer,
            LocalDate.of(2026, 8, 1),
            1,
            new BigDecimal("500.00"),
            BookingStatus.CONFIRMED,
            "CTR-TESTE2",
            "PIX");

    assertThat(booking.confirmCheckout(false, LocalDate.of(2026, 8, 10))).isFalse();
    assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
  }

  @Test
  void earlyCheckoutRequiresBothParties() {
    UserAccount owner = new UserAccount("Proprietário", "owner3@stowup.test", "hash", Role.OWNER);
    UserAccount customer =
        new UserAccount("Cliente", "customer3@stowup.test", "hash", Role.CUSTOMER);
    Space space =
        new Space(
            "Quarto",
            "Curitiba",
            "Batel",
            "Quarto",
            8,
            new BigDecimal("280.00"),
            0,
            0,
            0,
            true,
            "Quarto privativo",
            List.of("Área interna"),
            "#002B5B",
            "#FF5A1F",
            owner);
    Booking booking =
        new Booking(
            "RES-TESTE3",
            space,
            customer,
            LocalDate.of(2026, 8, 1),
            2,
            new BigDecimal("560.00"),
            BookingStatus.CONFIRMED,
            "CTR-TESTE3",
            "PIX");

    assertThat(booking.confirmCheckout(true, LocalDate.of(2026, 8, 20))).isFalse();
    assertThat(booking.confirmCheckout(false, LocalDate.of(2026, 8, 20))).isTrue();
    assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
  }
}
