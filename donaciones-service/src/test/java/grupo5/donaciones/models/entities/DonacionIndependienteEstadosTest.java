package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.AsignacionRealizada;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnDeposito;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnTraslado;
import grupo5.donaciones.models.entities.donacionesIndependientes.EntregaFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.Entregada;
import grupo5.donaciones.models.entities.donacionesIndependientes.ListaParaEntregar;
import grupo5.donaciones.models.entities.donacionesIndependientes.SolicitudCambioEstadoDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.Vencida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionAsignada;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionRecibida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoRutaIniciada;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonacionIndependienteEstadosTest {

  private static final String ACTOR = "SISTEMA";

  private DonacionIndependiente donacion;
  private NecesidadExtraordinaria receptor;

  @BeforeEach
  void setUp() {
    donacion = DonacionIndependienteMother.crearConCantidad(5);
    receptor = NecesidadMother.extraordinaria(UUID.randomUUID(), 10);
  }

  @Test
  void nuevaDonacionIndependiente_comienzaEnEstadoEnDeposito() {
    assertInstanceOf(EnDeposito.class, donacion.getEstadoActual());
    assertTrue(donacion.getDomainEvents().isEmpty());
  }

  @Test
  void asignar_desdeEnDeposito_transicionaCorrectamenteYEmiteEvento() {
    donacion.asignar(ACTOR, receptor);
    assertEquals(1, donacion.getHistorial().size());
    assertInstanceOf(AsignacionRealizada.class, donacion.getEstadoActual());
    assertEquals(1, donacion.getDomainEvents().size());
    assertInstanceOf(EventoDonacionAsignada.class, donacion.getDomainEvents().getFirst());
  }

  @Test
  void planificarRuta_luegoDe_asignar_transicionaAEnTraslado() {
    donacion.asignar(ACTOR, receptor);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    assertInstanceOf(EnTraslado.class, donacion.getEstadoActual());
  }

  @Test
  void iniciarRecorrido_luegoDePlanificarRuta_transicionaAListaParaEntregar() {
    donacion.asignar(ACTOR, receptor);
    donacion.planificarRuta(ACTOR);
    assertInstanceOf(ListaParaEntregar.class, donacion.getEstadoActual());
  }

  @Test
  void registrarFalla_desdeListaParaEntregar_transicionaAEntregaFallida() {
    donacion.asignar(ACTOR, receptor);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    donacion.registrarFalla("No había nadie", ACTOR);
    assertInstanceOf(EntregaFallida.class, donacion.getEstadoActual());
  }

  @Test
  void retornar_desdeEntregaFallida_vuelveAEnDeposito() {
    donacion.asignar(ACTOR, receptor);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    donacion.registrarFalla("No había nadie", ACTOR);
    donacion.retornar(ACTOR);
    assertInstanceOf(EnDeposito.class, donacion.getEstadoActual());
  }

  @Test
  void historial_registraCadaTransicion() {
    donacion.asignar(ACTOR, receptor);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    assertEquals(3, donacion.getHistorial().size());
  }

  @Test
  void historial_registraElActorEnCadaCambio() {
    donacion.asignar("admin1", receptor);
    assertEquals("admin1", donacion.getHistorial().getFirst().getActor());
  }

  @Test
  void vencer_desdeEnDeposito_transicionaCorrectamente() {
    donacion.vencer(ACTOR);
    assertFalse(donacion.getHistorial().isEmpty());
    assertInstanceOf(Vencida.class, donacion.getEstadoActual());
  }

  @Test
  void transicionInvalida_desdeEnDeposito_lanzaExcepcion() {
    assertThrows(BusinessStateException.class, () -> donacion.confirmarEntrega(ACTOR));
  }

  @Test
  void registrarFalla_sinJustificacion_lanzaExcepcion() {
    donacion.asignar(ACTOR, receptor);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    assertThrows(Exception.class, () -> donacion.registrarFalla(null, ACTOR));
  }

  @Test
  void cambiarEstado_conSolicitudCompleta_ejecutaTransicionYRegistraEventos() {
    // 1. Asignar
    SolicitudCambioEstadoDonacionIndependiente solAsignar =
        new SolicitudCambioEstadoDonacionIndependiente(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, receptor, ACTOR);
    donacion.cambiarEstado(solAsignar);
    assertInstanceOf(AsignacionRealizada.class, donacion.getEstadoActual());
    assertEquals(1, donacion.getDomainEvents().size());
    assertInstanceOf(EventoDonacionAsignada.class, donacion.getDomainEvents().getFirst());

    // 2. Planificar Ruta
    SolicitudCambioEstadoDonacionIndependiente solPlanificar =
        new SolicitudCambioEstadoDonacionIndependiente(
            TipoEstadoDonacion.LISTA_PARA_ENTREGAR, ACTOR);
    donacion.cambiarEstado(solPlanificar);
    assertInstanceOf(ListaParaEntregar.class, donacion.getEstadoActual());

    // 3. Iniciar Recorrido
    SolicitudCambioEstadoDonacionIndependiente solTraslado =
        new SolicitudCambioEstadoDonacionIndependiente(
            TipoEstadoDonacion.EN_TRASLADO, null, null, "http://mapa/ruta", null, null, ACTOR);
    donacion.cambiarEstado(solTraslado);
    assertInstanceOf(EnTraslado.class, donacion.getEstadoActual());
    assertEquals(2, donacion.getDomainEvents().size());
    assertInstanceOf(EventoRutaIniciada.class, donacion.getDomainEvents().get(1));

    // 4. Confirmar Entrega
    SolicitudCambioEstadoDonacionIndependiente solEntrega =
        new SolicitudCambioEstadoDonacionIndependiente(
            TipoEstadoDonacion.ENTREGADA, null, null, null, "ABC-123", null, ACTOR);
    donacion.cambiarEstado(solEntrega);
    assertInstanceOf(Entregada.class, donacion.getEstadoActual());
    assertEquals(3, donacion.getDomainEvents().size());
    assertInstanceOf(EventoDonacionRecibida.class, donacion.getDomainEvents().get(2));

    // Limpieza
    donacion.clearDomainEvents();
    assertTrue(donacion.getDomainEvents().isEmpty());
  }

  @Test
  void getDomainEvents_retornaListaInmodificable() {
    donacion.asignar(ACTOR, receptor);
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            donacion
                .getDomainEvents()
                .add(
                    new EventoDonacionAsignada(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())));
  }

  @Test
  void cambiarEstado_aAsignacionRealizadaSinNecesidad_lanzaExcepcion() {
    // Regresión de #762: pedir ASIGNACION_REALIZADA sin una Necesidad dejaría el agregado
    // "asignado" sin asignadaA ni necesidadId en el evento — un estado inconsistente.
    SolicitudCambioEstadoDonacionIndependiente solicitudSinNecesidad =
        new SolicitudCambioEstadoDonacionIndependiente(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, ACTOR);

    ValidationException ex =
        assertThrows(
            ValidationException.class, () -> donacion.cambiarEstado(solicitudSinNecesidad));

    assertEquals(ErrorCatalog.DONACION_INDEPENDIENTE_ASIGNACION_SIN_NECESIDAD, ex.getError());
    assertInstanceOf(EnDeposito.class, donacion.getEstadoActual());
  }

  @Test
  void getDomainEvents_debeSerUnaCopiaInmuneAMutacionesPosteriores() {
    // Regresión de #761: si getDomainEvents() devolviera una vista en vivo, mutar domainEvents
    // después de tomar el snapshot (p. ej. desde un listener reentrante) rompería la iteración
    // en curso con ConcurrentModificationException.
    donacion.asignar(ACTOR, receptor);
    var snapshot = donacion.getDomainEvents();
    int cantidadEnElSnapshot = snapshot.size();

    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR); // agrega EventoRutaIniciada a domainEvents "en vivo"

    assertEquals(cantidadEnElSnapshot, snapshot.size());
    assertDoesNotThrow(() -> snapshot.forEach(e -> {}));
  }
}
