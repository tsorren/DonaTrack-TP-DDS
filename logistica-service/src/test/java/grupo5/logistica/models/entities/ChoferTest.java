package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.choferes.CambioEstadoChofer;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChoferTest {

  // ========================= CHARACTERIZATION: constructor =========================

  @Test
  void testConstructorExitoso() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    assertNotNull(chofer.getId());
    assertEquals("Juan", chofer.getNombre());
    assertEquals("Perez", chofer.getApellido());
    assertEquals("LIC-12345", chofer.getLicencia());
    assertEquals("+541123456789", chofer.getTelefonoContacto());
    assertEquals(EstadoChofer.DISPONIBLE, chofer.getEstado());
    assertNull(chofer.getRutaId());
  }

  @Test
  void testConstructorConDatosNulosLanzaExcepcion() {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> new Chofer(null, "Perez", "LIC-12345", "+541123456789"));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testConstructorConDatosVaciosLanzaExcepcion() {
    ValidationException exception =
        assertThrows(
            ValidationException.class, () -> new Chofer("Juan", " ", "LIC-12345", "+541123456789"));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  // ========================= CHARACTERIZATION: actualizarLicencia =========================

  @Test
  void testActualizarLicenciaExitoso() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    chofer.actualizarLicencia("LIC-99999");
    assertEquals("LIC-99999", chofer.getLicencia());
  }

  @Test
  void testActualizarLicenciaNulaLanzaExcepcion() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    ValidationException exception =
        assertThrows(ValidationException.class, () -> chofer.actualizarLicencia(null));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testActualizarLicenciaVaciaLanzaExcepcion() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    ValidationException exception =
        assertThrows(ValidationException.class, () -> chofer.actualizarLicencia(""));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  // ========================= CHARACTERIZATION: actualizarTelefonoContacto
  // =========================

  @Test
  void testActualizarTelefonoContactoExitoso() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    chofer.actualizarTelefonoContacto("+541198765432");
    assertEquals("+541198765432", chofer.getTelefonoContacto());
  }

  @Test
  void testActualizarTelefonoContactoNuloLanzaExcepcion() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    ValidationException exception =
        assertThrows(ValidationException.class, () -> chofer.actualizarTelefonoContacto(null));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testActualizarTelefonoContactoVacioLanzaExcepcion() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    ValidationException exception =
        assertThrows(ValidationException.class, () -> chofer.actualizarTelefonoContacto("   "));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  // ========================= CHARACTERIZATION: asignarARuta =========================

  @Test
  void testAsignarARutaExitoso() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    UUID rutaId = UUID.randomUUID();
    chofer.asignarARuta(rutaId);
    assertEquals(EstadoChofer.EN_RUTA, chofer.getEstado());
    assertEquals(rutaId, chofer.getRutaId());
  }

  @Test
  void testAsignarARutaConIdNuloLanzaExcepcion() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    ValidationException exception =
        assertThrows(ValidationException.class, () -> chofer.asignarARuta(null));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testAsignarARutaCuandoYaEstaEnRutaLanzaExcepcion() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    chofer.asignarARuta(UUID.randomUUID());

    UUID otraRutaId = UUID.randomUUID();
    ValidationException exception =
        assertThrows(ValidationException.class, () -> chofer.asignarARuta(otraRutaId));
    assertEquals(ErrorCatalog.ESTADO_CHOFER_TRANSICION_INVALIDA, exception.getError());
  }

  // ========================= CHARACTERIZATION: completarRuta =========================

  @Test
  void testCompletarRutaExitoso() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    chofer.asignarARuta(UUID.randomUUID());
    chofer.completarRuta();
    assertEquals(EstadoChofer.DISPONIBLE, chofer.getEstado());
    assertNull(chofer.getRutaId());
  }

  @Test
  void testCompletarRutaCuandoEstaDisponibleLanzaExcepcion() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    ValidationException exception = assertThrows(ValidationException.class, chofer::completarRuta);
    assertEquals(ErrorCatalog.ESTADO_CHOFER_TRANSICION_INVALIDA, exception.getError());
  }

  // ========================= CHARACTERIZATION: habilitar / deshabilitar =========================

  @Test
  void testHabilitarYDeshabilitarChofer() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    assertTrue(chofer.estaDisponibleParaAsignar());

    chofer.deshabilitar();
    assertEquals(EstadoChofer.DESHABILITADO, chofer.getEstado());
    assertFalse(chofer.estaDisponibleParaAsignar());

    chofer.habilitar();
    assertEquals(EstadoChofer.DISPONIBLE, chofer.getEstado());
    assertTrue(chofer.estaDisponibleParaAsignar());
  }

  @Test
  void testHabilitarCuandoYaEstaHabilitadoLanzaExcepcion() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    ValidationException exception = assertThrows(ValidationException.class, chofer::habilitar);
    assertEquals(ErrorCatalog.ESTADO_CHOFER_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  void testDeshabilitarCuandoEstaEnRutaLanzaExcepcion() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    chofer.asignarARuta(UUID.randomUUID());

    ValidationException exception = assertThrows(ValidationException.class, chofer::deshabilitar);
    assertEquals(ErrorCatalog.ESTADO_CHOFER_TRANSICION_INVALIDA, exception.getError());
  }

  // ========================= HISTORIAL: estado inicial =========================

  @Test
  void testHistorialVacioAlCrear() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    assertTrue(chofer.getHistorialEstados().isEmpty());
  }

  // ========================= HISTORIAL: asignarARuta =========================

  @Test
  void testAsignarARutaRegistraEnHistorial() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    chofer.asignarARuta(UUID.randomUUID());

    List<CambioEstadoChofer> historial = chofer.getHistorialEstados();
    assertEquals(1, historial.size());
    assertEquals(EstadoChofer.DISPONIBLE, historial.get(0).estadoAnterior());
    assertEquals(EstadoChofer.EN_RUTA, historial.get(0).estadoNuevo());
    assertNotNull(historial.get(0).timestamp());
  }

  // ========================= HISTORIAL: completarRuta =========================

  @Test
  void testCompletarRutaRegistraEnHistorial() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    chofer.asignarARuta(UUID.randomUUID());
    chofer.completarRuta();

    List<CambioEstadoChofer> historial = chofer.getHistorialEstados();
    assertEquals(2, historial.size());
    assertEquals(EstadoChofer.EN_RUTA, historial.get(1).estadoAnterior());
    assertEquals(EstadoChofer.DISPONIBLE, historial.get(1).estadoNuevo());
  }

  // ========================= HISTORIAL: habilitar / deshabilitar =========================

  @Test
  void testDeshabilitarRegistraEnHistorial() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    chofer.deshabilitar();

    List<CambioEstadoChofer> historial = chofer.getHistorialEstados();
    assertEquals(1, historial.size());
    assertEquals(EstadoChofer.DISPONIBLE, historial.get(0).estadoAnterior());
    assertEquals(EstadoChofer.DESHABILITADO, historial.get(0).estadoNuevo());
  }

  @Test
  void testHabilitarRegistraEnHistorial() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    chofer.deshabilitar();
    chofer.habilitar();

    List<CambioEstadoChofer> historial = chofer.getHistorialEstados();
    assertEquals(2, historial.size());
    assertEquals(EstadoChofer.DESHABILITADO, historial.get(1).estadoAnterior());
    assertEquals(EstadoChofer.DISPONIBLE, historial.get(1).estadoNuevo());
  }

  // ========================= HISTORIAL: secuencia y orden =========================

  @Test
  void testHistorialReflecjaSecuenciaCompleta() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    chofer.deshabilitar();
    chofer.habilitar();
    chofer.asignarARuta(UUID.randomUUID());
    chofer.completarRuta();

    List<CambioEstadoChofer> historial = chofer.getHistorialEstados();
    assertEquals(4, historial.size());
    assertEquals(EstadoChofer.DISPONIBLE, historial.get(0).estadoAnterior());
    assertEquals(EstadoChofer.DESHABILITADO, historial.get(0).estadoNuevo());
    assertEquals(EstadoChofer.DESHABILITADO, historial.get(1).estadoAnterior());
    assertEquals(EstadoChofer.DISPONIBLE, historial.get(1).estadoNuevo());
    assertEquals(EstadoChofer.DISPONIBLE, historial.get(2).estadoAnterior());
    assertEquals(EstadoChofer.EN_RUTA, historial.get(2).estadoNuevo());
    assertEquals(EstadoChofer.EN_RUTA, historial.get(3).estadoAnterior());
    assertEquals(EstadoChofer.DISPONIBLE, historial.get(3).estadoNuevo());
  }

  // ========================= HISTORIAL: inmutabilidad =========================

  @Test
  void testGetHistorialEstadoEsInmutable() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    chofer.asignarARuta(UUID.randomUUID());

    List<CambioEstadoChofer> historial = chofer.getHistorialEstados();
    assertThrows(UnsupportedOperationException.class, () -> historial.add(null));
  }

  @Test
  void testGetHistorialEstadoEsSnapshot() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    chofer.asignarARuta(UUID.randomUUID());

    List<CambioEstadoChofer> snapshot = chofer.getHistorialEstados();
    assertEquals(1, snapshot.size());

    chofer.completarRuta();

    assertEquals(1, snapshot.size()); // snapshot no se modifica
    assertEquals(2, chofer.getHistorialEstados().size());
  }

  // ========================= HISTORIAL: transiciones fallidas no registran
  // =========================

  @Test
  void testTransicionFallidaNoRegistraEnHistorial() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    assertThrows(ValidationException.class, chofer::habilitar); // ya está DISPONIBLE

    assertTrue(chofer.getHistorialEstados().isEmpty());
  }
}
