package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.donaciones.models.entities.personas.direccion.Direccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DireccionTest {

  private Direccion direccion;

  @BeforeEach
  void setUp() {
    direccion = new Direccion("Calle Principal", 123, 2, "A", "Centro", "Buenos Aires");
  }

  @Test
  void crearDireccion_conDatosValidos_deberiaSuceder() {
    assertEquals("Calle Principal", direccion.getCalle());
    assertEquals(123, direccion.getAltura());
    assertEquals(2, direccion.getPiso());
    assertEquals("A", direccion.getDepartamento());
    assertEquals("Centro", direccion.getZona());
    assertEquals("Buenos Aires", direccion.getLocalidad());
  }

  @Test
  void crearDireccion_conCalleNula_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Direccion(null, 123, 2, "A", "Centro", "Buenos Aires"),
        "Debería lanzar error cuando la calle es nula");
  }

  @Test
  void crearDireccion_conCalleVacia_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Direccion("   ", 123, 2, "A", "Centro", "Buenos Aires"),
        "Debería lanzar error cuando la calle está vacía");
  }

  @Test
  void crearDireccion_conAlturaNula_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Direccion("Calle Principal", null, 2, "A", "Centro", "Buenos Aires"),
        "Debería lanzar error cuando la altura es nula");
  }

  @Test
  void crearDireccion_conAlturaCero_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Direccion("Calle Principal", 0, 2, "A", "Centro", "Buenos Aires"),
        "Debería lanzar error cuando la altura es cero");
  }

  @Test
  void crearDireccion_conAlturaNegativa_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Direccion("Calle Principal", -10, 2, "A", "Centro", "Buenos Aires"),
        "Debería lanzar error cuando la altura es negativa");
  }

  @Test
  void crearDireccion_conZonaNula_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Direccion("Calle Principal", 123, 2, "A", null, "Buenos Aires"),
        "Debería lanzar error cuando la zona es nula");
  }

  @Test
  void crearDireccion_conZonaVacia_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Direccion("Calle Principal", 123, 2, "A", "   ", "Buenos Aires"),
        "Debería lanzar error cuando la zona está vacía");
  }

  @Test
  void crearDireccion_conLocalidadNula_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Direccion("Calle Principal", 123, 2, "A", "Centro", null),
        "Debería lanzar error cuando la localidad es nula");
  }

  @Test
  void crearDireccion_conLocalidadVacia_deberiaLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Direccion("Calle Principal", 123, 2, "A", "Centro", "   "),
        "Debería lanzar error cuando la localidad está vacía");
  }

  @Test
  void crearDireccion_conPisoYDepartamentoOpcionales_deberiaSuceder() {
    Direccion direccionSinPiso =
        new Direccion("Calle Principal", 123, null, null, "Centro", "Buenos Aires");

    assertEquals(123, direccionSinPiso.getAltura());
    assertEquals(null, direccionSinPiso.getPiso());
    assertEquals(null, direccionSinPiso.getDepartamento());
  }

  @Test
  void anonimizar_deberiaLimpiarDatos() {
    direccion.anonimizar();

    assertEquals("ANONIMIZADO", direccion.getCalle());
    assertEquals(0, direccion.getAltura());
    assertEquals("ANONIMIZADO", direccion.getLocalidad());
  }
}
