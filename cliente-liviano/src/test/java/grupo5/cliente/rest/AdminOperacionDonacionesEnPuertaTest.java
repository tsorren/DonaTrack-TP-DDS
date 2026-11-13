package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.ConsultaDonaciones;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** HB-22: en Donaciones del admin, la que llegó a la puerta cuenta como "en camino". */
class AdminOperacionDonacionesEnPuertaTest {

  private static final UUID DONACION = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000d1");
  private static final UUID ENTIDAD = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000e1");

  @Test
  void laQueLlegoALaPuertaEstaEnCaminoYSuHistorialDiceAdondeLlego() {
    DonacionesApi api = mock(DonacionesApi.class);
    when(api.resumen())
        .thenReturn(
            new DonacionesApi.Resumen(Map.of("EN_PUERTA", 1L), null, null, null, null, null, null));
    DonacionesApi.Persona comedor =
        new DonacionesApi.Persona(
            "JURIDICA",
            ENTIDAD,
            null,
            null,
            null,
            null,
            null,
            null,
            "Comedor Ejemplo",
            null,
            null,
            null);
    when(api.entidad(ENTIDAD)).thenReturn(new DonacionesApi.Entidad(ENTIDAD, comedor, true));
    DonacionesApi.Cambio salio =
        new DonacionesApi.Cambio(
            "EnTraslado",
            LocalDateTime.parse("2026-11-05T14:30:00"),
            null,
            "ListaParaEntregar",
            null);
    DonacionesApi.Cambio llego =
        new DonacionesApi.Cambio(
            "EnPuerta", LocalDateTime.parse("2026-11-05T16:45:00"), null, "EnTraslado", null);
    DonacionesApi.Donacion arroz =
        new DonacionesApi.Donacion(
            DONACION,
            "Arroz",
            "EnPuerta",
            LocalDateTime.parse("2026-11-01T10:00:00"),
            List.of(salio, llego),
            List.of(),
            25,
            ENTIDAD,
            null,
            null,
            null,
            null);
    when(api.pagina(any())).thenReturn(ResponseEntity.ok(List.of(arroz)));
    when(api.donacionIndependiente(DONACION)).thenReturn(arroz);
    AdminOperacionDonaciones donaciones =
        new AdminOperacionDonaciones(api, LocalDateTime.parse("2026-11-10T18:30:00"));

    AdminDonacionesVista vista = donaciones.vista(new ConsultaDonaciones("camino", false, "", 1));
    AdminDonacionesVista.Donacion detalle = donaciones.detalle(DONACION.toString());

    assertEquals("camino", vista.donaciones().filas().get(0).estado());
    assertEquals("camino", detalle.estado());
    assertEquals(
        List.of(
            "1 de noviembre: llegó al depósito.",
            "5 de noviembre, 14:30: salió en camino.",
            "5 de noviembre, 16:45: llegó a Comedor Ejemplo."),
        detalle.historial());
    assertEquals(
        "0 en el depósito · 0 asignadas · 1 en camino · 0 entregas confirmadas en noviembre. Al 10"
            + " de noviembre de 2026, 18:30.",
        vista.resumen());
  }
}
