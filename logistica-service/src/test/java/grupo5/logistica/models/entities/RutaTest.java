package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class RutaTest {

  @Test
  public void testConstructorExitoso() {
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
  public void testConstructorConFechaNulaLanzaExcepcion() {
    UUID choferId = UUID.randomUUID();
    UUID camionId = UUID.randomUUID();

    ValidationException exception =
        assertThrows(ValidationException.class, () -> new Ruta(null, choferId, camionId));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  public void testConstructorConChoferNuloLanzaExcepcion() {
    ValidationException exception =
        assertThrows(
            ValidationException.class, () -> new Ruta(LocalDate.now(), null, UUID.randomUUID()));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  public void testAgregarEntregaExitoso() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    UUID entregaId = UUID.randomUUID();

    ruta.agregarEntrega(entregaId);

    assertEquals(1, ruta.getEntregas().size());
    assertTrue(ruta.getEntregas().contains(entregaId));
  }

  @Test
  public void testAgregarEntregaNulaLanzaExcepcion() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());

    ValidationException exception =
        assertThrows(ValidationException.class, () -> ruta.agregarEntrega(null));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  public void testAgregarEntregaDuplicadaLanzaExcepcion() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    UUID entregaId = UUID.randomUUID();
    ruta.agregarEntrega(entregaId);

    ValidationException exception =
        assertThrows(ValidationException.class, () -> ruta.agregarEntrega(entregaId));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  public void testIniciarRutaExitoso() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());

    ruta.iniciarRuta();

    assertEquals(EstadoRuta.EN_TRASLADO, ruta.getEstado());
    assertNotNull(ruta.getHoraInicioReal());
    assertNull(ruta.getHoraFinReal());
  }

  @Test
  public void testIniciarRutaSinEntregasLanzaExcepcion() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());

    ValidationException exception = assertThrows(ValidationException.class, ruta::iniciarRuta);
    assertEquals(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  public void testCompletarRutaExitoso() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());
    ruta.iniciarRuta();

    ruta.completarRuta();

    assertEquals(EstadoRuta.COMPLETADA, ruta.getEstado());
    assertNotNull(ruta.getHoraFinReal());
  }

  @Test
  public void testCompletarRutaCuandoEstaPendienteLanzaExcepcion() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());

    ValidationException exception = assertThrows(ValidationException.class, ruta::completarRuta);
    assertEquals(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  public void testColeccionEntregasInmutable() {
    Ruta ruta = new Ruta(LocalDate.now(), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(UUID.randomUUID());

    assertThrows(
        UnsupportedOperationException.class, () -> ruta.getEntregas().add(UUID.randomUUID()));
  }
}
