package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.AdminInicioVista.Camiones;
import grupo5.cliente.vistas.AdminInicioVista.Decisiones;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

/**
 * R11-21: el inicio del admin por temas. Las cifras salen del resumen de Donaciones (ninguna lista
 * se trae para contarla, tampoco las propuestas), con separador de miles en las frases, como mucho
 * 3 nombres de organizaciones y la tarjeta Camiones sola si Logística no responde.
 */
class AdminPersonasInicioTest {

  private static final LocalDateTime AHORA = LocalDateTime.parse("2026-10-15T18:31:00");
  private static final String AL = "15 de octubre de 2026, 18:31";

  private final DonacionesApi donaciones = mock(DonacionesApi.class);
  private final LogisticaApi logistica = mock(LogisticaApi.class);

  private static DonacionesApi.Resumen resumen(
      Map<String, Long> porEstado, long vencen, long propuestas) {
    return new DonacionesApi.Resumen(
        porEstado,
        new DonacionesApi.EntregadasEnElMes("2026-10", 9L),
        new DonacionesApi.PorVencer(
            7,
            vencen,
            vencen == 0
                ? null
                : new DonacionesApi.Proxima(
                    UUID.fromString("00000000-0000-4000-8000-0000000000d1"),
                    "Leche en polvo",
                    LocalDate.parse("2026-10-17"))),
        new DonacionesApi.CuantosDonantes(20_530L, 20_412L),
        1_046L,
        propuestas,
        OffsetDateTime.parse("2026-10-15T18:30:00-03:00"));
  }

  private AdminInicioVista inicio(List<String> pendientes) {
    return new AdminPersonasInicio(donaciones, logistica).inicio(AHORA, AL, pendientes);
  }

  @Test
  void lasCifrasVanPorTemaConLosEstadosAgrupadosComoEnLaLista() {
    when(donaciones.resumen())
        .thenReturn(
            resumen(
                Map.of(
                    "EN_DEPOSITO", 1_207L,
                    "ASIGNACION_REALIZADA", 3L,
                    "LISTA_PARA_ENTREGAR", 1L,
                    "EN_TRASLADO", 1L,
                    "EN_PUERTA", 1L,
                    "ENTREGA_FALLIDA", 1L,
                    "VENCIDA", 2L,
                    "ENTREGADA", 1_284L),
                4,
                2));
    when(logistica.camiones(any()))
        .thenReturn(
            List.of(
                new LogisticaApi.Camion(null, "AA000AA", null, null, null, "EN_RUTA", null),
                new LogisticaApi.Camion(null, "AA000AB", null, null, null, "DISPONIBLE", null)));

    AdminInicioVista inicio = inicio(List.of());

    assertEquals(
        new AdminInicioVista.Cifras(
            1_207,
            4,
            2,
            1,
            2,
            9,
            "en octubre, del 1 al 15",
            1_284,
            1_046,
            20_412,
            20_530,
            new Camiones(1, 1)),
        inicio.cifras());
    assertEquals("15 de octubre de 2026, 18:30", inicio.al());
    verify(donaciones, never()).propuestas(any());
  }

  @Test
  void lasFrasesLlevanSeparadorDeMilesYComoMuchoTresNombres() {
    when(donaciones.resumen()).thenReturn(resumen(Map.of("EN_DEPOSITO", 1_207L), 1_050L, 1_100L));
    when(logistica.camiones(any())).thenReturn(List.of());
    List<String> pendientes =
        List.of("Hogar Ejemplo Norte", "Merendero Ejemplo Este", "Comedor Ejemplo Oeste", "Otra");

    AdminInicioVista inicio = inicio(pendientes);

    assertEquals(
        "Hoy esperan tu decisión 4 organizaciones y 1.100 propuestas de asignación. Hay 1.207"
            + " donaciones sin asignar en el depósito; 1.050 vencen en los próximos 7 días.",
        inicio.resumen());
    assertEquals(
        new Decisiones(
            4,
            List.of("Hogar Ejemplo Norte", "Merendero Ejemplo Este", "Comedor Ejemplo Oeste"),
            1,
            1_207,
            1_050,
            "La primera, Leche en polvo, vence el 17 de octubre, en 2 días.",
            1_100),
        inicio.decisiones());
  }

  @Test
  void enSingularYSinVencimientos() {
    assertEquals(
        "Hoy espera tu decisión 1 organización. Hay 1 donación sin asignar en el depósito; vence"
            + " en los próximos 7 días.",
        AdminPersonasInicio.resumen(new Decisiones(1, List.of("Hogar"), 0, 1, 1, null, 0)));
    assertEquals(
        "Hay 7 donaciones sin asignar en el depósito; 1 vence en los próximos 7 días.",
        AdminPersonasInicio.resumen(new Decisiones(0, List.of(), 0, 7, 1, null, 0)));
    assertEquals(
        "Hay 7 donaciones sin asignar en el depósito.",
        AdminPersonasInicio.resumen(new Decisiones(0, List.of(), 0, 7, 0, null, 0)));
    assertNull(AdminPersonasInicio.resumen(new Decisiones(0, List.of(), 0, 0, 0, null, 0)));
  }

  @Test
  void siLogisticaNoRespondeSoloFaltanLosCamiones() {
    when(donaciones.resumen()).thenReturn(resumen(Map.of("EN_DEPOSITO", 7L), 0, 0));
    when(logistica.camiones(any())).thenThrow(new RestClientException("sin Logística"));

    AdminInicioVista inicio = inicio(List.of());

    assertNull(inicio.cifras().camiones());
    assertEquals(7, inicio.cifras().enDeposito());
    assertEquals(7, inicio.decisiones().sinAsignar());
  }

  @Test
  void elPrimerDiaDelMesYSinHoraDelResumen() {
    when(donaciones.resumen())
        .thenReturn(new DonacionesApi.Resumen(null, null, null, null, null, null, null));
    when(logistica.camiones(any())).thenReturn(List.of());

    AdminInicioVista inicio =
        new AdminPersonasInicio(donaciones, logistica)
            .inicio(
                LocalDateTime.parse("2026-11-01T09:00:00"),
                "1 de noviembre de 2026, 09:00",
                List.of());

    assertEquals("el 1 de noviembre", inicio.cifras().delMes());
    assertEquals("1 de noviembre de 2026, 09:00", inicio.al());
    assertEquals(0, inicio.cifras().entregadasEnTotal());
  }
}
