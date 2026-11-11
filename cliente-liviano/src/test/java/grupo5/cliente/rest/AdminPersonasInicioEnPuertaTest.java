package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.cliente.vistas.AdminEntidadesVista;
import grupo5.cliente.vistas.AdminInicioVista;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** HB-22: en el inicio del admin, la donación en la puerta suma a la cifra de "en camino". */
class AdminPersonasInicioEnPuertaTest {

  private static DonacionesApi.Donacion donacion(String n, String estado) {
    return new DonacionesApi.Donacion(
        UUID.fromString("6f1c2c1e-0000-4000-8000-00000000000" + n),
        "Arroz",
        estado,
        null,
        List.of(),
        List.of(),
        1,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void laQueEstaEnLaPuertaCuentaComoEnCamino() {
    DonacionesApi donaciones = mock(DonacionesApi.class);
    LogisticaApi logistica = mock(LogisticaApi.class);
    when(donaciones.buscar(any()))
        .thenReturn(
            List.of(
                donacion("1", "EnTraslado"),
                donacion("2", "EnPuerta"),
                donacion("3", "EnDeposito")));
    when(donaciones.propuestas()).thenReturn(List.of());
    when(logistica.camiones(any())).thenReturn(List.of());

    AdminInicioVista inicio =
        new AdminPersonasInicio(donaciones, logistica)
            .inicio(
                LocalDateTime.parse("2026-11-10T18:30:00"),
                "10 de noviembre de 2026, 18:30",
                new AdminEntidadesVista(0, "", List.of()),
                0);

    assertEquals(2, inicio.cifras().enCamino());
    assertEquals(1, inicio.cifras().enDeposito());
  }
}
