package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.cliente.vistas.AdminInicioVista;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * HB-22: en el inicio del admin, la donación en la puerta suma a la cifra de "en camino". Desde
 * R11-01 las cifras salen del resumen de Donaciones: ninguna lista se trae para contarla.
 */
class AdminPersonasInicioEnPuertaTest {

  @Test
  void laQueEstaEnLaPuertaCuentaComoEnCamino() {
    DonacionesApi donaciones = mock(DonacionesApi.class);
    LogisticaApi logistica = mock(LogisticaApi.class);
    when(donaciones.resumen())
        .thenReturn(
            new DonacionesApi.Resumen(
                Map.of("EN_TRASLADO", 1L, "EN_PUERTA", 1L, "EN_DEPOSITO", 1L),
                null,
                null,
                null,
                null,
                null,
                null));
    when(logistica.camiones(any())).thenReturn(List.of());

    AdminInicioVista inicio =
        new AdminPersonasInicio(donaciones, logistica)
            .inicio(
                LocalDateTime.parse("2026-11-10T18:30:00"),
                "10 de noviembre de 2026, 18:30",
                List.of());

    assertEquals(2, inicio.cifras().enCamino());
    assertEquals(1, inicio.cifras().enDeposito());
    assertEquals(0, inicio.decisiones().vencen());
    verify(donaciones, never()).pagina(any());
    verify(donaciones, never()).donantes(any());
    verify(donaciones, never()).propuestas(any());
  }
}
