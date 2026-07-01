package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RutaTest {

  @Test
  void testConstructorExitoso() {
    LocalDate fecha = LocalDate.now();
    UUID choferId = UUID.randomUUID();
    UUID camionId = UUID.randomUUID();

    Ruta ruta = new Ruta(fecha, choferId, camionId);

    assertNotNull(ruta.getId());
    assertEquals(fecha, ruta.getFecha());
    assertEquals(choferId, ruta.getChoferId());
    assertEquals(camionId, ruta.getCamionId());
    assertEquals(EstadoRuta.PENDIENTE, ruta.getEstado());
    assertTrue(ruta.getEntregas().isEmpty());
  }

  @Test
  void testConstructorConFechaNulaLanzaExcepcion() {
    UUID choferId = UUID.randomUUID();
    UUID camionId = UUID.randomUUID();

    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Ruta(null, choferId, camionId));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testConstructorConChoferNuloLanzaExcepcion() {
    LocalDate fecha = LocalDate.now();
    UUID camionId = UUID.randomUUID();
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Ruta(fecha, null, camionId));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testAgregarEntregaExitoso() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    UUID entregaId = UUID.randomUUID();

    ruta.agregarEntrega(entregaId);

    assertEquals(1, ruta.getEntregas().size());
    assertTrue(ruta.getEntregas().contains(entregaId));
  }

  @Test
  void testAgregarEntregaNulaLanzaExcepcion() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());

    ValidationException exception =
        assertThrows(ValidationException.class, () -> ruta.agregarEntrega(null));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  void testAgregarEntregaDuplicadaLanzaExcepcion() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    UUID entregaId = UUID.randomUUID();
    ruta.agregarEntrega(entregaId);

    ValidationException exception =
        assertThrows(ValidationException.class, () -> ruta.agregarEntrega(entregaId));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  void testIniciarRutaExitoso() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());

    ruta.iniciarRuta();

    assertEquals(EstadoRuta.EN_TRASLADO, ruta.getEstado());
    assertNotNull(ruta.getHoraInicioReal());
    assertNull(ruta.getHoraFinReal());
  }

  @Test
  void testIniciarRutaSinEntregasLanzaExcepcion() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());

    ValidationException exception = assertThrows(ValidationException.class, ruta::iniciarRuta);
    assertEquals(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  void testCompletarRutaExitoso() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());
    ruta.iniciarRuta();

    ruta.completarRuta();

    assertEquals(EstadoRuta.COMPLETADA, ruta.getEstado());
    assertNotNull(ruta.getHoraFinReal());
  }

  @Test
  void testCompletarRutaCuandoEstaPendienteLanzaExcepcion() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());

    ValidationException exception = assertThrows(ValidationException.class, ruta::completarRuta);
    assertEquals(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  void testColeccionEntregasInmutable() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());

    List<UUID> entregas = ruta.getEntregas();
    UUID nuevaEntregaId = UUID.randomUUID();
    assertThrows(UnsupportedOperationException.class, () -> entregas.add(nuevaEntregaId));
  }
}
