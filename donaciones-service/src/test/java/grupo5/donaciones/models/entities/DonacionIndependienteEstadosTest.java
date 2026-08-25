package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.*;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.*;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonacionIndependienteEstadosTest {

  private static final String ACTOR = "SISTEMA";
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private DonacionIndependiente donacion;
  private NecesidadExtraordinaria receptor;

  @BeforeEach
  void setUp() {
    Humana humana = new Humana("Juan", "Pérez", LocalDate.of(1990, Month.JANUARY, 1));
    Donante donante = new Donante(humana.getId());
    Donacion donacionOriginal = new Donacion(donante.getId());

    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "Ropa de Invierno");
    Bien bien = new Bien("Abrigo", "abrigo.png", TEST_DATE.plusMonths(6), Estado.NUEVO, 1.0, 1.0);
    BienNormalizado bienNormalizado =
        new BienNormalizado(
            bien, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);

    ItemDonacionNormalizado itemNormalizado =
        new ItemDonacionNormalizado(donacionOriginal.getId(), bienNormalizado, 5);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(itemNormalizado.getBien(), 5);

    donacion = new DonacionIndependiente(donacionOriginal.getId(), List.of(item));
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
            TipoEstadoDonacion.ASIGNACION_REALIZADA, ACTOR);
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
}
