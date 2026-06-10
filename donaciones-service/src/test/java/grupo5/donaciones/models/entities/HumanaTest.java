package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;

class HumanaTest {

  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  @Test
  void crearHumana_conDatosValidos_deberiaSuceder() {
    LocalDate fechaNac = LocalDate.of(1990, Month.JANUARY, 1);
    Humana humana = new Humana("Juan", "Pérez", fechaNac);

    assertEquals("Juan", humana.getNombre());
    assertEquals("Pérez", humana.getApellido());
    assertEquals(fechaNac, humana.getFechaNacimiento());
  }

  @Test
  void crearHumana_conNombreNulo_deberiaLanzarExcepcion() {
    LocalDate fechaNac = LocalDate.of(1990, Month.JANUARY, 1);
    assertThrows(
        ValidationException.class,
        () -> new Humana(null, "Pérez", fechaNac),
        "Debería lanzar error cuando el nombre es nulo");
  }

  @Test
  void crearHumana_conNombreVacio_deberiaLanzarExcepcion() {
    LocalDate fechaNac = LocalDate.of(1990, Month.JANUARY, 1);
    assertThrows(
        ValidationException.class,
        () -> new Humana("   ", "Pérez", fechaNac),
        "Debería lanzar error cuando el nombre está vacío");
  }

  @Test
  void crearHumana_conApellidoNulo_deberiaLanzarExcepcion() {
    LocalDate fechaNac = LocalDate.of(1990, Month.JANUARY, 1);
    assertThrows(
        ValidationException.class,
        () -> new Humana("Juan", null, fechaNac),
        "Debería lanzar error cuando el apellido es nulo");
  }

  @Test
  void crearHumana_conApellidoVacio_deberiaLanzarExcepcion() {
    LocalDate fechaNac = LocalDate.of(1990, Month.JANUARY, 1);
    assertThrows(
        ValidationException.class,
        () -> new Humana("Juan", "   ", fechaNac),
        "Debería lanzar error cuando el apellido está vacío");
  }

  @Test
  void crearHumana_conFechaNacimientoFutura_deberiaLanzarExcepcion() {
    LocalDate fechaFutura = LocalDate.of(2050, Month.JANUARY, 1);

    assertThrows(
        ValidationException.class,
        () -> new Humana("Juan", "Pérez", fechaFutura),
        "Debería lanzar error cuando la fecha de nacimiento es futura");
  }

  @Test
  void crearHumana_conFechaNacimientoNula_deberiaSuceder() {
    Humana humana = new Humana("Juan", "Pérez", null);

    assertEquals("Juan", humana.getNombre());
    assertNull(humana.getFechaNacimiento());
  }

  @Test
  void crearHumana_conFechaNacimientoPasada_deberiaSuceder() {
    LocalDate fechaPasada = LocalDate.of(1950, Month.JANUARY, 1);
    Humana humana = new Humana("Juan", "Pérez", fechaPasada);

    assertEquals(fechaPasada, humana.getFechaNacimiento());
  }

  @Test
  void crearHumana_conFechaNacimientoDeHoy_deberiaSuceder() {
    Humana humana = new Humana("Juan", "Pérez", TEST_DATE);

    assertEquals(TEST_DATE, humana.getFechaNacimiento());
  }
}
