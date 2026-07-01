package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CamionTest {

  @Test
  public void testConstructorExitoso() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    assertNotNull(camion.getId());
    assertEquals("ABC-123", camion.getPatente());
    assertEquals(15.5f, camion.getCapacidadVolumen());
    assertEquals(1500f, camion.getCapacidadKG());
    assertEquals(2.5f, camion.getAltura());
    assertEquals(EstadoCamion.DISPONIBLE, camion.getEstado());
    assertNull(camion.getRutaId());
  }

  @Test
  public void testConstructorConPatenteNulaLanzaExcepcion() {
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Camion(null, 15.5f, 1500f, 2.5f));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  public void testConstructorConPatenteVaciaLanzaExcepcion() {
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Camion("   ", 15.5f, 1500f, 2.5f));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  public void testConstructorConCapacidadVolumenInvalidaLanzaExcepcion() {
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Camion("ABC-123", 0f, 1500f, 2.5f));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  public void testConstructorConCapacidadKGInvalidaLanzaExcepcion() {
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Camion("ABC-123", 15.5f, -100f, 2.5f));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  public void testConstructorConAlturaInvalidaLanzaExcepcion() {
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Camion("ABC-123", 15.5f, 1500f, 0f));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  public void testAsignarARutaExitoso() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    UUID rutaId = UUID.randomUUID();
    camion.asignarARuta(rutaId);
    assertEquals(EstadoCamion.EN_RUTA, camion.getEstado());
    assertEquals(rutaId, camion.getRutaId());
  }

  @Test
  public void testAsignarARutaConIdNuloLanzaExcepcion() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    ValidationException exception =
        assertThrows(ValidationException.class, () -> camion.asignarARuta(null));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  public void testAsignarARutaCuandoYaEstaEnRutaLanzaExcepcion() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.asignarARuta(UUID.randomUUID());

    ValidationException exception =
        assertThrows(ValidationException.class, () -> camion.asignarARuta(UUID.randomUUID()));
    assertEquals(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  public void testCompletarRutaExitoso() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.asignarARuta(UUID.randomUUID());
    camion.completarRuta();
    assertEquals(EstadoCamion.DISPONIBLE, camion.getEstado());
    assertNull(camion.getRutaId());
  }

  @Test
  public void testCompletarRutaCuandoEstaDisponibleLanzaExcepcion() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    ValidationException exception = assertThrows(ValidationException.class, camion::completarRuta);
    assertEquals(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  public void testHabilitarYDeshabilitarCamion() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    assertTrue(camion.estaDisponibleParaAsignar());

    camion.deshabilitar();
    assertEquals(EstadoCamion.DESHABILITADO, camion.getEstado());
    assertFalse(camion.estaDisponibleParaAsignar());

    camion.habilitar();
    assertEquals(EstadoCamion.DISPONIBLE, camion.getEstado());
    assertTrue(camion.estaDisponibleParaAsignar());
  }

  @Test
  public void testHabilitarCamionCuandoYaEstaHabilitadoLanzaExcepcion() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    ValidationException exception = assertThrows(ValidationException.class, camion::habilitar);
    assertEquals(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  public void testDeshabilitarCamionCuandoEstaEnRutaLanzaExcepcion() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.asignarARuta(UUID.randomUUID());

    ValidationException exception = assertThrows(ValidationException.class, camion::deshabilitar);
    assertEquals(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA, exception.getError());
  }
}
