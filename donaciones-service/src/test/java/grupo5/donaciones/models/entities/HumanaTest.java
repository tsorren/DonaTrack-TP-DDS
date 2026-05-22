package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class HumanaTest {

  @Test
  void crearHumana_conDatosValidos_deberiaSuceder() {
    Humana humana = new Humana("Juan", "Pérez", LocalDate.of(1990, 1, 1));

    assertEquals("Juan", humana.getNombre());
    assertEquals("Pérez", humana.getApellido());
    assertEquals(LocalDate.of(1990, 1, 1), humana.getFechaNacimiento());
  }

  @Test
  void crearHumana_conNombreNulo_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Humana(null, "Pérez", LocalDate.of(1990, 1, 1)),
        "Debería lanzar error cuando el nombre es nulo");
  }

  @Test
  void crearHumana_conNombreVacio_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Humana("   ", "Pérez", LocalDate.of(1990, 1, 1)),
        "Debería lanzar error cuando el nombre está vacío");
  }

  @Test
  void crearHumana_conApellidoNulo_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Humana("Juan", null, LocalDate.of(1990, 1, 1)),
        "Debería lanzar error cuando el apellido es nulo");
  }

  @Test
  void crearHumana_conApellidoVacio_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Humana("Juan", "   ", LocalDate.of(1990, 1, 1)),
        "Debería lanzar error cuando el apellido está vacío");
  }

  @Test
  void crearHumana_conFechaNacimientoFutura_deberiaLanzarExcepcion() {
    LocalDate fechaFutura = LocalDate.now().plusDays(1);

    assertThrows(
        IllegalArgumentException.class,
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
    LocalDate fechaPasada = LocalDate.of(1950, 1, 1);
    Humana humana = new Humana("Juan", "Pérez", fechaPasada);

    assertEquals(fechaPasada, humana.getFechaNacimiento());
  }

  @Test
  void crearHumana_conFechaNacimientoDeHoy_deberiaSuceder() {
    LocalDate hoy = LocalDate.now();
    Humana humana = new Humana("Juan", "Pérez", hoy);

    assertEquals(hoy, humana.getFechaNacimiento());
  }
}
