package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.entregas.CambioEstadoEntrega;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class EntregaTest {

  private Direccion createTestDireccion() {
    Pais pais = new Pais("Argentina");
    Provincia prov = new Provincia("Buenos Aires", pais);
    Localidad loc = new Localidad("Lanus", prov);
    return new Direccion("Calle Falsa", 123, null, null, "1824", loc);
  }

  @Test
  public void testConstructorExitoso() {
    UUID idRuta = UUID.randomUUID();
    UUID idDonacion = UUID.randomUUID();
    UUID idBeneficiaria = UUID.randomUUID();
    Direccion destino = createTestDireccion();

    Entrega entrega = new Entrega(idRuta, idDonacion, idBeneficiaria, destino, 10.5f, 0.5f);

    assertNotNull(entrega.getId());
    assertEquals(idRuta, entrega.getIdRuta());
    assertEquals(idDonacion, entrega.getIdDonacion());
    assertEquals(idBeneficiaria, entrega.getIdBeneficiaria());
    assertEquals(destino, entrega.getDestino());
    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstadoActual());
    assertEquals(10.5f, entrega.getPesoTotalKG());
    assertEquals(0.5f, entrega.getVolumenTotalM3());
    assertTrue(entrega.getHistorialEstado().isEmpty());
  }

  @Test
  public void testConstructorConIdDonacionNuloLanzaExcepcion() {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () ->
                new Entrega(
                    UUID.randomUUID(), null, UUID.randomUUID(), createTestDireccion(), 10f, 1f));
    assertEquals(ErrorCatalog.ARGUMENTO_NULO, exception.getError());
  }

  @Test
  public void testConstructorConPesoNegativoLanzaExcepcion() {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () ->
                new Entrega(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    createTestDireccion(),
                    -5f,
                    1f));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  public void testFlujoFelizDeTrazabilidad() {
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            10f,
            1f);

    // 1. Iniciar ruta
    entrega.iniciarRuta("Chofer Jose");
    assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstadoActual());
    assertNotNull(entrega.getHoraSalida());
    assertEquals(1, entrega.getHistorialEstado().size());
    CambioEstadoEntrega cambio1 = entrega.getHistorialEstado().get(0);
    assertEquals(EstadoEntrega.PENDIENTE, cambio1.estadoAnterior());
    assertEquals(EstadoEntrega.EN_TRASLADO, cambio1.estadoNuevo());
    assertEquals("Chofer Jose", cambio1.actor());

    // 2. Confirmar entrega
    entrega.confirmarEntrega("Comedor Infantil");
    assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstadoActual());
    assertNotNull(entrega.getHoraArribo());
    assertEquals(2, entrega.getHistorialEstado().size());
    CambioEstadoEntrega cambio2 = entrega.getHistorialEstado().get(1);
    assertEquals(EstadoEntrega.EN_TRASLADO, cambio2.estadoAnterior());
    assertEquals(EstadoEntrega.ENTREGADA, cambio2.estadoNuevo());
    assertEquals("Comedor Infantil", cambio2.actor());

    // 3. Adjuntar foto
    entrega.adjuntarFotoRecepcion("http://images.com/recepcion.jpg");
    assertEquals("http://images.com/recepcion.jpg", entrega.getFotoRecepcionUrl());
  }

  @Test
  public void testFlujoAlternativoNoRecibidoYRegreso() {
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            10f,
            1f);

    entrega.iniciarRuta("Chofer Jose");

    // Negar entrega
    entrega.negarEntrega("Comedor Infantil");
    assertEquals(
        EstadoEntrega.REVISION,
        entrega.getEstadoActual()); // Pasa a NO_RECIBIDA y luego inmediatamente a REVISION en
    // negarEntrega()
    assertEquals(
        3,
        entrega
            .getHistorialEstado()
            .size()); // Registro de EN_TRASLADO -> NO_RECIBIDA y NO_RECIBIDA -> REVISION

    // Regresar al deposito
    entrega.regresarAlDeposito("Admin Carlos");
    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstadoActual());
    assertNull(entrega.getHoraArribo());
    assertNull(entrega.getHoraSalida());
  }

  @Test
  public void testIniciarRutaConChoferVacioLanzaExcepcion() {
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            10f,
            1f);

    ValidationException exception =
        assertThrows(ValidationException.class, () -> entrega.iniciarRuta(" "));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, exception.getError());
  }

  @Test
  public void testTransicionInvalidaLanzaExcepcion() {
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            10f,
            1f);

    // Confirmar entrega directamente sin iniciar ruta
    ValidationException exception =
        assertThrows(ValidationException.class, () -> entrega.confirmarEntrega("Comedor"));
    assertEquals(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA, exception.getError());
  }

  @Test
  public void testHistorialEstadoEsInmutable() {
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            createTestDireccion(),
            10f,
            1f);
    entrega.iniciarRuta("Chofer Jose");

    assertThrows(
        UnsupportedOperationException.class,
        () ->
            entrega
                .getHistorialEstado()
                .add(
                    new CambioEstadoEntrega(
                        EstadoEntrega.PENDIENTE, EstadoEntrega.ENTREGADA, null, "hack")));
  }
}
