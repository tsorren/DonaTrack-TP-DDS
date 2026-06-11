package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.personas.Direccion;
import grupo5.donaciones.models.entities.personas.Localidad;
import grupo5.donaciones.models.entities.personas.Pais;
import grupo5.donaciones.models.entities.personas.Provincia;
import grupo5.donaciones.models.privacidad.Anonimizable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DireccionTest {

  private Direccion direccion;
  private Localidad localidad;

  @BeforeEach
  void setUp() {
    Pais pais = new Pais();
    pais.setNombre("Argentina");
    Provincia provincia = new Provincia();
    provincia.setNombre("Buenos Aires");
    provincia.setPais(pais);
    localidad = new Localidad();
    localidad.setNombre("Buenos Aires");
    localidad.setProvincia(provincia);
    direccion = new Direccion("Calle Principal", 123, 2, "A", "C1000", localidad);
  }

  @Test
  void crearDireccion_conDatosValidos_deberiaSuceder() {
    assertEquals("Calle Principal", direccion.getCalle());
    assertEquals(123, direccion.getAltura());
    assertEquals(2, direccion.getPiso());
    assertEquals("A", direccion.getDepartamento());
    assertEquals("C1000", direccion.getCodigoPostal());
    assertEquals(localidad, direccion.getLocalidad());
  }

  @Test
  void crearDireccion_conCalleNula_deberiaLanzarExcepcion() {
    assertThrows(
        ValidationException.class,
        () -> new Direccion(null, 123, 2, "A", "C1000", localidad),
        "Debería lanzar error cuando la calle es nula");
  }

  @Test
  void crearDireccion_conCalleVacia_deberiaLanzarExcepcion() {
    assertThrows(
        ValidationException.class,
        () -> new Direccion("   ", 123, 2, "A", "C1000", localidad),
        "Debería lanzar error cuando la calle está vacía");
  }

  @Test
  void crearDireccion_conAlturaNula_deberiaLanzarExcepcion() {
    assertThrows(
        ValidationException.class,
        () -> new Direccion("Calle Principal", null, 2, "A", "C1000", localidad),
        "Debería lanzar error cuando la altura es nula");
  }

  @Test
  void crearDireccion_conAlturaCero_deberiaLanzarExcepcion() {
    assertThrows(
        ValidationException.class,
        () -> new Direccion("Calle Principal", 0, 2, "A", "C1000", localidad),
        "Debería lanzar error cuando la altura es cero");
  }

  @Test
  void crearDireccion_conAlturaNegativa_deberiaLanzarExcepcion() {
    assertThrows(
        ValidationException.class,
        () -> new Direccion("Calle Principal", -10, 2, "A", "C1000", localidad),
        "Debería lanzar error cuando la altura es negativa");
  }

  @Test
  void crearDireccion_conCodigoPostalNulo_deberiaLanzarExcepcion() {
    assertThrows(
        ValidationException.class,
        () -> new Direccion("Calle Principal", 123, 2, "A", null, localidad),
        "Debería lanzar error cuando el código postal es nulo");
  }

  @Test
  void crearDireccion_conCodigoPostalVacio_deberiaLanzarExcepcion() {
    assertThrows(
        ValidationException.class,
        () -> new Direccion("Calle Principal", 123, 2, "A", "   ", localidad),
        "Debería lanzar error cuando el código postal está vacío");
  }

  @Test
  void crearDireccion_conLocalidadNula_deberiaLanzarExcepcion() {
    assertThrows(
        ValidationException.class,
        () -> new Direccion("Calle Principal", 123, 2, "A", "C1000", null),
        "Debería lanzar error cuando la localidad es nula");
  }

  @Test
  void crearDireccion_conPisoYDepartamentoOpcionales_deberiaSuceder() {
    Direccion direccionSinPiso =
        new Direccion("Calle Principal", 123, null, null, "C1000", localidad);

    assertEquals(123, direccionSinPiso.getAltura());
    assertNull(direccionSinPiso.getPiso());
    assertNull(direccionSinPiso.getDepartamento());
  }

  @Test
  void anonimizar_deberiaLimpiarDatos() {
    direccion.anonimizar();

    assertEquals(Anonimizable.VALOR_STRING, direccion.getCalle());
    assertEquals(Anonimizable.VALOR_NUMERICO, direccion.getAltura());
    assertEquals(Anonimizable.VALOR_STRING, direccion.getCodigoPostal());
    assertEquals(Anonimizable.VALOR_STRING, direccion.getDepartamento());
    assertEquals(Anonimizable.VALOR_NUMERICO, direccion.getPiso());
    assertEquals(Anonimizable.VALOR_STRING, direccion.getLocalidad().getNombre());
    assertEquals(Anonimizable.VALOR_STRING, direccion.getLocalidad().getProvincia().getNombre());
    assertEquals(
        Anonimizable.VALOR_STRING, direccion.getLocalidad().getProvincia().getPais().getNombre());
  }
}
