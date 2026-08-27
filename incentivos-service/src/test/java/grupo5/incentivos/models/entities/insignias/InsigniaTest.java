package grupo5.incentivos.models.entities.insignias;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;

class InsigniaTest {

  @Test
  void insignia_deberiaConstruirseCorrectamenteConCamposValidos() {
    String nombre = "Racha Inicial";
    String descripcion = "3 meses";
    String imagenUrl = "/img.png";
    Insignia insignia = new Insignia(nombre, descripcion, imagenUrl);
    assertEquals(nombre, insignia.nombre());
    assertEquals(descripcion, insignia.descripcion());
    assertEquals(imagenUrl, insignia.imagenUrl());
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
    String nombre = "Racha";
    String descripcion = "desc";
    String imagenUrl = "/img.png";
    LocalDate fecha = LocalDate.of(2026, Month.JUNE, 1);
    InsigniaGanada ganada = new InsigniaGanada(nombre, descripcion, imagenUrl, true, fecha);
    assertEquals(nombre, ganada.nombre());
    assertEquals(descripcion, ganada.descripcion());
    assertEquals(imagenUrl, ganada.imagenUrl());
    assertTrue(ganada.visible());
    assertEquals(fecha, ganada.fechaObtenida());
  }

  @Test
  void insigniaGanada_deberiaLanzarExcepcionConNombreNuloOVacio() {
    LocalDate ahora = LocalDate.now();
    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> new InsigniaGanada(null, "desc", "/img.png", true, ahora));
    assertEquals(ErrorCatalog.INSIGNIA_SIN_NOMBRE, ex.getError());
  }

  @Test
  void insigniaGanada_ocultadaYVisible_deberianEvolucionarInmutablemente() {
    LocalDate fecha = LocalDate.of(2026, Month.JUNE, 1);
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
