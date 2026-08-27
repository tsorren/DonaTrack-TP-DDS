package grupo5.incentivos.models.entities.insignias;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InsigniaTest {

  @Test
  void insignia_deberiaConstruirseCorrectamenteConCamposValidos() {
    Insignia insignia = new Insignia("Racha Inicial", "3 meses", "/img.png");
    assertEquals("Racha Inicial", insignia.nombre());
    assertEquals("3 meses", insignia.descripcion());
    assertEquals("/img.png", insignia.imagenUrl());
  }

  @Test
  void insignia_deberiaLanzarExcepcionConNombreNuloOVacio() {
    ValidationException ex1 =
        assertThrows(ValidationException.class, () -> new Insignia(null, "desc", "/img.png"));
    assertEquals(ErrorCatalog.INSIGNIA_SIN_NOMBRE, ex1.getError());

    ValidationException ex2 =
        assertThrows(ValidationException.class, () -> new Insignia("   ", "desc", "/img.png"));
    assertEquals(ErrorCatalog.INSIGNIA_SIN_NOMBRE, ex2.getError());
  }

  @Test
  void insigniaGanada_deberiaConstruirseCorrectamente() {
    LocalDate fecha = LocalDate.of(2026, 6, 1);
    InsigniaGanada ganada = new InsigniaGanada("Racha", "desc", "/img.png", true, fecha);
    assertEquals("Racha", ganada.nombre());
    assertEquals("desc", ganada.descripcion());
    assertEquals("/img.png", ganada.imagenUrl());
    assertTrue(ganada.visible());
    assertEquals(fecha, ganada.fechaObtenida());
  }

  @Test
  void insigniaGanada_deberiaLanzarExcepcionConNombreNuloOVacio() {
    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> new InsigniaGanada(null, "desc", "/img.png", true, LocalDate.now()));
    assertEquals(ErrorCatalog.INSIGNIA_SIN_NOMBRE, ex.getError());
  }

  @Test
  void insigniaGanada_ocultadaYVisible_deberianEvolucionarInmutablemente() {
    LocalDate fecha = LocalDate.of(2026, 6, 1);
    InsigniaGanada visible = new InsigniaGanada("Racha", "desc", "/img.png", true, fecha);
    InsigniaGanada ocultada = visible.ocultada();

    assertFalse(ocultada.visible());
    assertTrue(visible.visible());
    assertEquals(visible.nombre(), ocultada.nombre());
    assertEquals(visible.fechaObtenida(), ocultada.fechaObtenida());

    InsigniaGanada revivida = ocultada.mostrada();
    assertTrue(revivida.visible());

    InsigniaGanada toggle = visible.conVisibilidad(false);
    assertFalse(toggle.visible());
  }
}
