package grupo5.incentivos.models.entities.donante;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventoDonacionTest {

  @Test
  void builder_deberiaConstruirEventoValido() {
    LocalDate fecha = LocalDate.of(2026, 6, 15);
    UUID donacionId = UUID.randomUUID();
    EventoDonacion evento =
        EventoDonacion.builder()
            .donacionId(donacionId)
            .fecha(fecha)
            .cantidadBienes(10)
            .categorias(List.of("alimentos", "ropa"))
            .build();

    assertEquals(donacionId, evento.getDonacionId());
    assertEquals(fecha, evento.getFecha());
    assertEquals(10, evento.getCantidadBienes());
    assertEquals(List.of("alimentos", "ropa"), evento.getCategorias());
  }

  @Test
  void builder_deberiaManejarCategoriasNulasComoListaVacia() {
    EventoDonacion evento =
        EventoDonacion.builder()
            .donacionId(UUID.randomUUID())
            .fecha(LocalDate.of(2026, 6, 15))
            .cantidadBienes(5)
            .categorias(null)
            .build();

    assertNotNull(evento.getCategorias());
    assertTrue(evento.getCategorias().isEmpty());
  }

  @Test
  void builder_deberiaLanzarExcepcionConFechaNula() {
    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () ->
                EventoDonacion.builder()
                    .donacionId(UUID.randomUUID())
                    .fecha(null)
                    .cantidadBienes(5)
                    .build());
    assertEquals(ErrorCatalog.EVENTO_DONACION_SIN_FECHA, ex.getError());
  }

  @Test
  void builder_deberiaLanzarExcepcionConCantidadBienesNegativaOCero() {
    ValidationException ex1 =
        assertThrows(
            ValidationException.class,
            () ->
                EventoDonacion.builder()
                    .donacionId(UUID.randomUUID())
                    .fecha(LocalDate.of(2026, 6, 15))
                    .cantidadBienes(0)
                    .build());
    assertEquals(ErrorCatalog.EVENTO_DONACION_CANTIDAD_INVALIDA, ex1.getError());

    ValidationException ex2 =
        assertThrows(
            ValidationException.class,
            () ->
                EventoDonacion.builder()
                    .donacionId(UUID.randomUUID())
                    .fecha(LocalDate.of(2026, 6, 15))
                    .cantidadBienes(-3)
                    .build());
    assertEquals(ErrorCatalog.EVENTO_DONACION_CANTIDAD_INVALIDA, ex2.getError());
  }
}
