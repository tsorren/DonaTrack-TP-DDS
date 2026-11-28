package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.entregas.ConfirmacionRecepcion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.entregas.GestorDeEntregas;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** HG-03: nombre opcional de quien recibió la entrega, dato personal privado (DA-10). */
class QuienRecibioTest {

  private static final List<String> FOTOS = List.of("https://storage.donatrack.org/f-1.jpg");
  private static final String NOMBRE = "Persona Sintetica";
  private static final String ORG = "org-1";

  private static Entrega enTraslado() {
    Localidad lanus = new Localidad("Lanus", new Provincia("Buenos Aires", new Pais("Argentina")));
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new Direccion("Calle Falsa", 123, null, null, "1824", lanus),
            10f,
            1f);
    entrega.iniciarRuta("Chofer");
    return entrega;
  }

  @Test
  void confirmaConElNombreRecortadoYElActorSigueSiendoLaOrganizacion() {
    Entrega entrega = enTraslado();

    GestorDeEntregas.cambiarEstado(
        new ConfirmacionRecepcion(entrega, ORG, FOTOS, null, null, "  " + NOMBRE + "  "));

    assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstadoActual());
    assertEquals(NOMBRE, entrega.getRecibidoPor());
    assertEquals(ORG, entrega.getHistorialEstado().getLast().actor());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   "})
  void sinNombreOConUnoEnBlancoQuedaSinNombre(String nombre) {
    Entrega entrega = enTraslado();

    entrega.confirmarEntrega(ORG, FOTOS, null, null, nombre);

    assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstadoActual());
    assertNull(entrega.getRecibidoPor());
  }

  @Test
  void lasFirmasAnterioresConfirmanSinNombre() {
    Entrega entrega = enTraslado();

    entrega.confirmarEntrega(ORG, FOTOS, "R-1", null);

    assertNull(entrega.getRecibidoPor());
    assertNull(new ConfirmacionRecepcion(entrega, ORG, FOTOS, null, null).recibidoPor());
  }

  @Test
  void aceptaHasta80CaracteresYRechazaMasSinCambiarElEstado() {
    Entrega conTope = enTraslado();
    conTope.confirmarEntrega(ORG, FOTOS, null, null, "a".repeat(80));
    assertEquals(80, conTope.getRecibidoPor().length());

    Entrega conBordes = enTraslado();
    conBordes.confirmarEntrega(ORG, FOTOS, null, null, "  " + "a".repeat(80) + "  ");
    assertEquals("a".repeat(80), conBordes.getRecibidoPor());

    Entrega largo = enTraslado();
    String nombre = "a".repeat(81);
    assertThrows(
        ValidationException.class, () -> largo.confirmarEntrega(ORG, FOTOS, null, null, nombre));
    assertEquals(EstadoEntrega.EN_TRASLADO, largo.getEstadoActual());
    assertNull(largo.getRecibidoPor());
    assertTrue(largo.getDomainEvents().isEmpty());
  }

  @Test
  void elEventoDeEntregaConfirmadaNoLlevaElNombre() throws JsonProcessingException {
    Entrega entrega = enTraslado();

    entrega.confirmarEntrega(ORG, FOTOS, null, null, NOMBRE);

    EntregaConfirmada evento =
        assertInstanceOf(EntregaConfirmada.class, entrega.getDomainEvents().getFirst());
    String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(evento);
    assertTrue(json.contains(evento.getEntregaId().toString()));
    assertFalse(json.contains(NOMBRE));
  }

  @Test
  void reconstituyeElNombreDesdeLaPersistencia() {
    Entrega entrega = enTraslado();

    entrega.reconstituirRecepcion("R-1", null, NOMBRE);

    assertEquals(NOMBRE, entrega.getRecibidoPor());
    assertEquals("R-1", entrega.getNumeroRemito());
  }
}
