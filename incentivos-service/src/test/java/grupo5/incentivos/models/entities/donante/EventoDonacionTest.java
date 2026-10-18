package grupo5.incentivos.models.entities.donante;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventoDonacionTest {

  @Test
  void builder_deberiaConstruirEventoValido() {
    LocalDate fecha = LocalDate.of(2026, Month.JUNE, 15);
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
            .fecha(LocalDate.of(2026, Month.JUNE, 15))
            .cantidadBienes(5)
            .categorias(null)
            .build();

    assertNotNull(evento.getCategorias());
    assertTrue(evento.getCategorias().isEmpty());
  }

  @Test
  void builder_deberiaLanzarExcepcionConFechaNula() {
    var builder =
        EventoDonacion.builder().donacionId(UUID.randomUUID()).fecha(null).cantidadBienes(5);
    ValidationException ex = assertThrows(ValidationException.class, builder::build);
    assertEquals(ErrorCatalog.EVENTO_DONACION_SIN_FECHA, ex.getError());
  }

  @Test
  void builder_deberiaLanzarExcepcionConCantidadBienesNegativaOCero() {
    LocalDate fecha = LocalDate.of(2026, Month.JUNE, 15);
    UUID donacionId = UUID.randomUUID();

    var builder1 = EventoDonacion.builder().donacionId(donacionId).fecha(fecha).cantidadBienes(0);
    ValidationException ex1 = assertThrows(ValidationException.class, builder1::build);
    assertEquals(ErrorCatalog.EVENTO_DONACION_CANTIDAD_INVALIDA, ex1.getError());

    var builder2 = EventoDonacion.builder().donacionId(donacionId).fecha(fecha).cantidadBienes(-3);
    ValidationException ex2 = assertThrows(ValidationException.class, builder2::build);
    assertEquals(ErrorCatalog.EVENTO_DONACION_CANTIDAD_INVALIDA, ex2.getError());
  }
}
