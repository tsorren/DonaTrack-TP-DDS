package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.choferes.Chofer;
import org.junit.jupiter.api.Test;

class ChoferTest {

  @Test
  void testConstructorExitoso() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-12345", "+541123456789");
    assertNotNull(chofer.getId());
    assertEquals("Juan", chofer.getNombre());
    assertEquals("Perez", chofer.getApellido());
    assertEquals("LIC-12345", chofer.getLicencia());
    assertEquals("+541123456789", chofer.getTelefonoContacto());
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
}
