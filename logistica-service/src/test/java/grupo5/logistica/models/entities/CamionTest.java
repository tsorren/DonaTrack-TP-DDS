package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.CambioEstadoCamion;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CamionTest {

  // ========================= CHARACTERIZATION: constructor =========================

  @Test
  void testConstructorExitoso() {
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
  void testConstructorConPatenteNulaLanzaExcepcion() {
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Camion(null, 15.5f, 1500f, 2.5f));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testConstructorConPatenteVaciaLanzaExcepcion() {
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Camion("   ", 15.5f, 1500f, 2.5f));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  void testConstructorConCapacidadVolumenInvalidaLanzaExcepcion() {
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Camion("ABC-123", 0f, 1500f, 2.5f));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  void testConstructorConCapacidadKGInvalidaLanzaExcepcion() {
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Camion("ABC-123", 15.5f, -100f, 2.5f));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  void testConstructorConAlturaInvalidaLanzaExcepcion() {
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Camion("ABC-123", 15.5f, 1500f, 0f));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  // ========================= CHARACTERIZATION: asignarARuta =========================

  @Test
  void testAsignarARutaExitoso() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    UUID rutaId = UUID.randomUUID();
    camion.asignarARuta(rutaId);
    assertEquals(EstadoCamion.EN_RUTA, camion.getEstado());
    assertEquals(rutaId, camion.getRutaId());
  }

  @Test
  void testAsignarARutaConIdNuloLanzaExcepcion() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    ValidationException exception =
        assertThrows(ValidationException.class, () -> camion.asignarARuta(null));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testAsignarARutaCuandoYaEstaEnRutaLanzaExcepcion() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.asignarARuta(UUID.randomUUID());

    UUID rutaId = UUID.randomUUID();
    ValidationException exception =
        assertThrows(ValidationException.class, () -> camion.asignarARuta(rutaId));
    assertEquals(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA, exception.getError());
  }

  // ========================= CHARACTERIZATION: completarRuta =========================

  @Test
  void testCompletarRutaExitoso() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.asignarARuta(UUID.randomUUID());
    camion.completarRuta();
    assertEquals(EstadoCamion.DISPONIBLE, camion.getEstado());
    assertNull(camion.getRutaId());
  }

  @Test
  void testCompletarRutaCuandoEstaDisponibleLanzaExcepcion() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    ValidationException exception = assertThrows(ValidationException.class, camion::completarRuta);
    assertEquals(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA, exception.getError());
  }

  // ========================= CHARACTERIZATION: habilitar / deshabilitar =========================

  @Test
  void testHabilitarYDeshabilitarCamion() {
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
  void testHabilitarCamionCuandoYaEstaHabilitadoLanzaExcepcion() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    ValidationException exception = assertThrows(ValidationException.class, camion::habilitar);
    assertEquals(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  void testDeshabilitarCamionCuandoEstaEnRutaLanzaExcepcion() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.asignarARuta(UUID.randomUUID());

    ValidationException exception = assertThrows(ValidationException.class, camion::deshabilitar);
    assertEquals(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA, exception.getError());
  }

  // ========================= HISTORIAL: estado inicial =========================

  @Test
  void testHistorialVacioAlCrear() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    assertTrue(camion.getHistorialEstado().isEmpty());
  }

  // ========================= HISTORIAL: asignarARuta =========================

  @Test
  void testAsignarARutaRegistraEnHistorial() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.asignarARuta(UUID.randomUUID());

    List<CambioEstadoCamion> historial = camion.getHistorialEstado();
    assertEquals(1, historial.size());
    assertEquals(EstadoCamion.DISPONIBLE, historial.get(0).estadoAnterior());
    assertEquals(EstadoCamion.EN_RUTA, historial.get(0).estadoNuevo());
    assertNotNull(historial.get(0).timestamp());
  }

  // ========================= HISTORIAL: completarRuta =========================

  @Test
  void testCompletarRutaRegistraEnHistorial() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.asignarARuta(UUID.randomUUID());
    camion.completarRuta();

    List<CambioEstadoCamion> historial = camion.getHistorialEstado();
    assertEquals(2, historial.size());
    assertEquals(EstadoCamion.EN_RUTA, historial.get(1).estadoAnterior());
    assertEquals(EstadoCamion.DISPONIBLE, historial.get(1).estadoNuevo());
  }

  // ========================= HISTORIAL: habilitar / deshabilitar =========================

  @Test
  void testDeshabilitarRegistraEnHistorial() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.deshabilitar();

    List<CambioEstadoCamion> historial = camion.getHistorialEstado();
    assertEquals(1, historial.size());
    assertEquals(EstadoCamion.DISPONIBLE, historial.get(0).estadoAnterior());
    assertEquals(EstadoCamion.DESHABILITADO, historial.get(0).estadoNuevo());
  }

  @Test
  void testHabilitarRegistraEnHistorial() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.deshabilitar();
    camion.habilitar();

    List<CambioEstadoCamion> historial = camion.getHistorialEstado();
    assertEquals(2, historial.size());
    assertEquals(EstadoCamion.DESHABILITADO, historial.get(1).estadoAnterior());
    assertEquals(EstadoCamion.DISPONIBLE, historial.get(1).estadoNuevo());
  }

  // ========================= HISTORIAL: secuencia y orden =========================

  @Test
  void testHistorialReflecjaSecuenciaCompleta() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.deshabilitar();
    camion.habilitar();
    camion.asignarARuta(UUID.randomUUID());
    camion.completarRuta();

    List<CambioEstadoCamion> historial = camion.getHistorialEstado();
    assertEquals(4, historial.size());
    assertEquals(EstadoCamion.DISPONIBLE, historial.get(0).estadoAnterior());
    assertEquals(EstadoCamion.DESHABILITADO, historial.get(0).estadoNuevo());
    assertEquals(EstadoCamion.DESHABILITADO, historial.get(1).estadoAnterior());
    assertEquals(EstadoCamion.DISPONIBLE, historial.get(1).estadoNuevo());
    assertEquals(EstadoCamion.DISPONIBLE, historial.get(2).estadoAnterior());
    assertEquals(EstadoCamion.EN_RUTA, historial.get(2).estadoNuevo());
    assertEquals(EstadoCamion.EN_RUTA, historial.get(3).estadoAnterior());
    assertEquals(EstadoCamion.DISPONIBLE, historial.get(3).estadoNuevo());
  }

  // ========================= HISTORIAL: inmutabilidad =========================

  @Test
  void testGetHistorialEstadoEsInmutable() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.asignarARuta(UUID.randomUUID());

    List<CambioEstadoCamion> historial = camion.getHistorialEstado();
    assertThrows(UnsupportedOperationException.class, () -> historial.add(null));
  }

  @Test
  void testGetHistorialEstadoEsSnapshot() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    camion.asignarARuta(UUID.randomUUID());

    List<CambioEstadoCamion> snapshot = camion.getHistorialEstado();
    assertEquals(1, snapshot.size());

    camion.completarRuta();

    assertEquals(1, snapshot.size()); // snapshot no se modifica
    assertEquals(2, camion.getHistorialEstado().size());
  }

  // ========================= HISTORIAL: transiciones fallidas no registran
  // =========================

  @Test
  void testTransicionFallidaNoRegistraEnHistorial() {
    Camion camion = new Camion("ABC-123", 15.5f, 1500f, 2.5f);
    assertThrows(ValidationException.class, camion::habilitar); // ya está DISPONIBLE

    assertTrue(camion.getHistorialEstado().isEmpty());
  }
}
