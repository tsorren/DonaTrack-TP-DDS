package grupo5.donaciones.infrastructure.logistica;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaCancelada;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaEnPuerta;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.infrastructure.idempotency.EventoConsumido;
import grupo5.donaciones.infrastructure.idempotency.IEventosConsumidosRepository;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.EstadoDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import grupo5.donaciones.services.logistica.ILogisticaBroker;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ProcesadorEventosLogisticaTest {

  @Mock private IDonacionesIndependientesService donacionesIndependientesService;
  @Mock private IEventosConsumidosRepository eventosConsumidosRepository;
  @Mock private IDonacionesIndependientesRepository donacionesIndependientesRepository;
  @Mock private ILogisticaBroker broker;
  // Sin transaction manager, como el perfil en memoria: getIfAvailable devuelve null
  @Mock private ObjectProvider<PlatformTransactionManager> transactionManager;

  private static final String ORIGEN = "cola-de-prueba";

  @InjectMocks private ProcesadorEventosLogistica procesador;

  @Test
  void onRutaAsignada_cuandoEventoNuevo_aplicaCambioEstadoYRegistraComoConsumido() {
    UUID donacionId = UUID.randomUUID();
    EventoRutaAsignada evento =
        new EventoRutaAsignada(UUID.randomUUID(), donacionId, LocalDateTime.now());

    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());

    procesador.procesarRutaAsignada(evento, ORIGEN);

    verify(donacionesIndependientesService)
        .cambiarEstado(
            eq(donacionId), any(CambioEstadoDonacionIndependienteRequestDTO.class), any());
    verify(eventosConsumidosRepository).registrar(any(EventoConsumido.class));
  }

  @Test
  void onRutaAsignada_cuandoEventoDuplicado_saltaSinProcesar() {
    EventoRutaAsignada evento =
        new EventoRutaAsignada(UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now());

    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(true);

    procesador.procesarRutaAsignada(evento, ORIGEN);

    verify(donacionesIndependientesService, never()).cambiarEstado(any(), any(), any());
    verify(eventosConsumidosRepository, never()).registrar(any());
  }

  @Test
  void onEntregaExitosa_conCantidadRecibida_laRegistraDespuesDelCambioDeEstado() {
    UUID donacionId = UUID.randomUUID();
    EventoEntregaExitosa evento =
        new EventoEntregaExitosa(
            UUID.randomUUID(),
            donacionId,
            UUID.randomUUID(),
            "ABC-123",
            LocalDateTime.now(),
            new EventoEntregaExitosa.Reporte("INCOMPLETA", null, 12.5));
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());

    procesador.procesarEntregaExitosa(evento, ORIGEN);

    var orden = inOrder(donacionesIndependientesService, eventosConsumidosRepository);
    orden.verify(donacionesIndependientesService).cambiarEstado(eq(donacionId), any(), any());
    orden.verify(donacionesIndependientesService).registrarCantidadRecibida(donacionId, 12.5);
    orden.verify(eventosConsumidosRepository).registrar(any(EventoConsumido.class));
  }

  @Test
  void onEntregaExitosa_sinCantidadRecibida_noDescuentaNada() {
    UUID donacionId = UUID.randomUUID();
    EventoEntregaExitosa evento =
        new EventoEntregaExitosa(
            UUID.randomUUID(),
            donacionId,
            UUID.randomUUID(),
            "ABC-123",
            LocalDateTime.now(),
            new EventoEntregaExitosa.Reporte("DANADA", "Caja mojada", null));
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());

    procesador.procesarEntregaExitosa(evento, ORIGEN);

    verify(donacionesIndependientesService).cambiarEstado(eq(donacionId), any(), any());
    verify(donacionesIndependientesService, never()).registrarCantidadRecibida(any(), anyDouble());
  }

  @Test
  void onEntregaExitosa_conDonacionYaEntregada_igualRegistraLaCantidadRecibida() {
    UUID donacionId = UUID.randomUUID();
    EventoEntregaExitosa evento =
        new EventoEntregaExitosa(
            UUID.randomUUID(),
            donacionId,
            UUID.randomUUID(),
            "ABC-123",
            LocalDateTime.now(),
            new EventoEntregaExitosa.Reporte("INCOMPLETA", null, 4.0));
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    EstadoDonacionIndependiente estado = mock(EstadoDonacionIndependiente.class);
    when(estado.getTipo()).thenReturn(TipoEstadoDonacion.ENTREGADA);
    DonacionIndependiente donacion = mock(DonacionIndependiente.class);
    when(donacion.getEstadoActual()).thenReturn(estado);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.of(donacion));

    procesador.procesarEntregaExitosa(evento, ORIGEN);

    verify(donacionesIndependientesService, never()).cambiarEstado(any(), any(), any());
    verify(donacionesIndependientesService).registrarCantidadRecibida(donacionId, 4.0);
  }

  @Test
  void onEntregaExitosa_duplicado_noVuelveADescontar() {
    EventoEntregaExitosa evento =
        new EventoEntregaExitosa(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ABC-123",
            LocalDateTime.now(),
            new EventoEntregaExitosa.Reporte("INCOMPLETA", null, 3.0));
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(true);

    procesador.procesarEntregaExitosa(evento, ORIGEN);

    verify(donacionesIndependientesService, never()).registrarCantidadRecibida(any(), anyDouble());
  }

  @Test
  void onEntregaExitosa_cuandoDonacionYaEnEstadoDestino_registraYDescartaSinLlamarServicio() {
    UUID donacionId = UUID.randomUUID();
    EventoEntregaExitosa evento =
        new EventoEntregaExitosa(
            UUID.randomUUID(), donacionId, UUID.randomUUID(), "ABC-123", LocalDateTime.now());

    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);

    EstadoDonacionIndependiente estado = mock(EstadoDonacionIndependiente.class);
    when(estado.getTipo()).thenReturn(TipoEstadoDonacion.ENTREGADA);
    DonacionIndependiente donacion = mock(DonacionIndependiente.class);
    when(donacion.getEstadoActual()).thenReturn(estado);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.of(donacion));

    procesador.procesarEntregaExitosa(evento, ORIGEN);

    verify(donacionesIndependientesService, never()).cambiarEstado(any(), any(), any());
    verify(eventosConsumidosRepository).registrar(any(EventoConsumido.class));
  }

  @Test
  void onRutaIniciada_cuandoEventoNuevo_procesaCadaDonacionIndependientemente() {
    UUID donacionId1 = UUID.randomUUID();
    UUID donacionId2 = UUID.randomUUID();
    EventoRutaIniciada evento =
        new EventoRutaIniciada(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "XYZ-999",
            List.of(donacionId1, donacionId2),
            LocalDateTime.now(),
            "http://mapa");

    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(any())).thenReturn(Optional.empty());

    procesador.procesarRutaIniciada(evento, ORIGEN);

    verify(donacionesIndependientesService, times(2)).cambiarEstado(any(), any(), any());
    verify(eventosConsumidosRepository, times(2)).registrar(any(EventoConsumido.class));
  }

  @Test
  void onRutaIniciada_cuandoUnaDonacionDuplicadaYOtraNo_procesaSoloLaNueva() {
    UUID donacionDuplicadaId = UUID.randomUUID();
    UUID donacionNuevaId = UUID.randomUUID();
    EventoRutaIniciada evento =
        new EventoRutaIniciada(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "XYZ-999",
            List.of(donacionDuplicadaId, donacionNuevaId),
            LocalDateTime.now(),
            "http://mapa");

    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), eq(donacionDuplicadaId)))
        .thenReturn(true);
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), eq(donacionNuevaId)))
        .thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacionNuevaId)).thenReturn(Optional.empty());

    procesador.procesarRutaIniciada(evento, ORIGEN);

    verify(donacionesIndependientesService, times(1))
        .cambiarEstado(eq(donacionNuevaId), any(), any());
    verify(donacionesIndependientesService, never())
        .cambiarEstado(eq(donacionDuplicadaId), any(), any());
  }

  @Test
  void onEntregaExitosa_cuandoServicioFalla_noPropagaNiRegistraComoConsumido() {
    UUID donacionId = UUID.randomUUID();
    EventoEntregaExitosa evento =
        new EventoEntregaExitosa(
            UUID.randomUUID(), donacionId, UUID.randomUUID(), "ABC-123", LocalDateTime.now());

    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());
    when(donacionesIndependientesService.cambiarEstado(any(), any(), any()))
        .thenThrow(new RuntimeException("Fallo al cambiar estado"));

    assertDoesNotThrow(() -> procesador.procesarEntregaExitosa(evento, ORIGEN));

    // No queda como consumido: si el mismo evento vuelve a llegar, se procesa de nuevo.
    verify(eventosConsumidosRepository, never()).registrar(any());
  }

  @Test
  void onRutaAsignada_siOtraOperacionGuardoLaDonacionALaVez_reintentaYAplicaElCambio() {
    UUID donacionId = UUID.randomUUID();
    EventoRutaAsignada evento =
        new EventoRutaAsignada(UUID.randomUUID(), donacionId, LocalDateTime.now());
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());
    when(donacionesIndependientesService.cambiarEstado(eq(donacionId), any(), any()))
        .thenThrow(
            new BusinessStateException(
                ErrorCatalog.DONACION_INDEPENDIENTE_MODIFICADA_CONCURRENTEMENTE))
        .thenReturn(null);

    procesador.procesarRutaAsignada(evento, ORIGEN);

    verify(donacionesIndependientesService, times(2)).cambiarEstado(eq(donacionId), any(), any());
    verify(eventosConsumidosRepository, times(1)).registrar(any(EventoConsumido.class));
  }

  @Test
  void onRutaAsignada_siElConflictSeRepite_dejaDeReintentarYNoRegistraElEvento() {
    UUID donacionId = UUID.randomUUID();
    EventoRutaAsignada evento =
        new EventoRutaAsignada(UUID.randomUUID(), donacionId, LocalDateTime.now());
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());
    when(donacionesIndependientesService.cambiarEstado(eq(donacionId), any(), any()))
        .thenThrow(
            new BusinessStateException(
                ErrorCatalog.DONACION_INDEPENDIENTE_MODIFICADA_CONCURRENTEMENTE));

    procesador.procesarRutaAsignada(evento, ORIGEN);

    verify(donacionesIndependientesService, times(3)).cambiarEstado(eq(donacionId), any(), any());
    verify(eventosConsumidosRepository, never()).registrar(any());
  }

  // HB-01: réplica de las fotos de recepción para las destacadas

  private static final List<String> FOTOS =
      List.of("http://localhost:8086/api/entregas/1/fotos/a", "https://ejemplo.org/b.jpg");

  private EventoEntregaExitosa exitosa(
      UUID donacionId, EventoEntregaExitosa.Reporte reporte, List<String> fotos) {
    return new EventoEntregaExitosa(
        UUID.randomUUID(),
        donacionId,
        UUID.randomUUID(),
        "ABC-123",
        LocalDateTime.of(2026, 11, 8, 13, 0),
        reporte,
        fotos);
  }

  @Test
  void onEntregaExitosa_sinReporteYConFotos_lasReplicaEnLaMismaTransaccion() {
    UUID donacionId = UUID.randomUUID();
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());

    procesador.procesarEntregaExitosa(exitosa(donacionId, null, FOTOS), ORIGEN);

    var orden = inOrder(donacionesIndependientesService, eventosConsumidosRepository);
    orden.verify(donacionesIndependientesService).cambiarEstado(eq(donacionId), any(), any());
    orden
        .verify(donacionesIndependientesService)
        .registrarFotosDeRecepcion(donacionId, LocalDateTime.of(2026, 11, 8, 13, 0), FOTOS);
    orden.verify(eventosConsumidosRepository).registrar(any(EventoConsumido.class));
  }

  @Test
  void onEntregaExitosa_conReporte_noReplicaLasFotos() {
    UUID donacionId = UUID.randomUUID();
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());

    procesador.procesarEntregaExitosa(
        exitosa(donacionId, new EventoEntregaExitosa.Reporte("DANADA", null, null), FOTOS), ORIGEN);

    verify(donacionesIndependientesService).cambiarEstado(eq(donacionId), any(), any());
    verify(donacionesIndependientesService, never())
        .registrarFotosDeRecepcion(any(), any(), anyList());
  }

  @Test
  void onEntregaExitosa_mensajeViejoSinFotos_igualPasaAEntregadaSinReplicar() {
    UUID donacionId = UUID.randomUUID();
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());

    procesador.procesarEntregaExitosa(exitosa(donacionId, null, null), ORIGEN);

    verify(donacionesIndependientesService).cambiarEstado(eq(donacionId), any(), any());
    verify(donacionesIndependientesService, never())
        .registrarFotosDeRecepcion(any(), any(), anyList());
    verify(eventosConsumidosRepository).registrar(any(EventoConsumido.class));
  }

  @Test
  void onEntregaExitosa_descartaLasUrlsVaciasYSinNingunaNoReplica() {
    UUID donacionId = UUID.randomUUID();
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());
    List<String> soloVacias = new ArrayList<>();
    soloVacias.add(" ");
    soloVacias.add(null);

    procesador.procesarEntregaExitosa(exitosa(donacionId, null, soloVacias), ORIGEN);

    verify(donacionesIndependientesService, never())
        .registrarFotosDeRecepcion(any(), any(), anyList());
  }

  @Test
  void onEntregaExitosa_duplicado_noVuelveAReplicar() {
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(true);

    procesador.procesarEntregaExitosa(exitosa(UUID.randomUUID(), null, FOTOS), ORIGEN);

    verify(donacionesIndependientesService, never())
        .registrarFotosDeRecepcion(any(), any(), anyList());
  }

  @Test
  void entregaExitosa_jsonConCamposDesconocidos_seLeeIgual() {
    String json =
        "{\"entregaId\":\""
            + UUID.randomUUID()
            + "\",\"donacionIndependienteId\":\""
            + UUID.randomUUID()
            + "\",\"fechaEntrega\":\"2026-11-08T13:00:00\","
            + "\"fotosRecepcion\":[\"https://ejemplo.org/a.jpg\"],\"campoNuevo\":1}";

    EventoEntregaExitosa leido =
        assertDoesNotThrow(
            () ->
                JsonMapper.builder()
                    .enable(
                        tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build()
                    .readValue(json, EventoEntregaExitosa.class));
    assertEquals(List.of("https://ejemplo.org/a.jpg"), leido.fotosRecepcion());
  }

  // ===================== entrega.cancelada (HB-19) =====================

  private static EventoEntregaCancelada cancelada(UUID donacionId) {
    return new EventoEntregaCancelada(
        UUID.randomUUID(), donacionId, "NECESIDAD_DADA_DE_BAJA", LocalDateTime.now());
  }

  @Test
  void entregaCancelada_liberaLaDonacionCierraLaSolicitudYRegistraElEvento() {
    DonacionIndependiente donacion =
        DonacionIndependienteMother.listaParaEntregar(
            2, NecesidadMother.extraordinaria(UUID.randomUUID(), 10));
    donacion.pedirCancelacion();
    EventoEntregaCancelada evento = cancelada(donacion.getId());
    when(eventosConsumidosRepository.yaFueConsumido(
            "EntregaCancelada", evento.entregaId(), donacion.getId()))
        .thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacion.getId()))
        .thenReturn(Optional.of(donacion));

    procesador.procesarEntregaCancelada(evento, ORIGEN);

    assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacion.getEstadoActual().getTipo());
    assertNull(donacion.getNecesidadId());
    assertFalse(donacion.isCancelacionPedida());
    verify(donacionesIndependientesRepository).save(donacion);
    verify(broker).registrarCancelacionConfirmada(donacion.getId());
    verify(eventosConsumidosRepository)
        .registrar(
            argThat(
                (EventoConsumido e) ->
                    "EntregaCancelada".equals(e.eventType())
                        && evento.entregaId().equals(e.businessId())));
  }

  @Test
  void entregaCancelada_siLaDonacionYaSalio_noLaTocaPeroRegistraElEvento() {
    DonacionIndependiente enCamino =
        DonacionIndependienteMother.enTraslado(
            2, NecesidadMother.extraordinaria(UUID.randomUUID(), 10));
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(enCamino.getId()))
        .thenReturn(Optional.of(enCamino));

    procesador.procesarEntregaCancelada(cancelada(enCamino.getId()), ORIGEN);

    assertEquals(TipoEstadoDonacion.EN_TRASLADO, enCamino.getEstadoActual().getTipo());
    verify(donacionesIndependientesRepository, never()).save(any());
    verifyNoInteractions(broker);
    verify(eventosConsumidosRepository).registrar(any(EventoConsumido.class));
  }

  @Test
  void entregaCancelada_duplicada_seIgnora() {
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(true);

    procesador.procesarEntregaCancelada(cancelada(UUID.randomUUID()), ORIGEN);

    verifyNoInteractions(donacionesIndependientesRepository, broker);
    verify(eventosConsumidosRepository, never()).registrar(any());
  }

  // ===================== entrega.en-puerta (HB-22) =====================

  private static EventoEntregaEnPuerta enPuerta(UUID donacionId) {
    return new EventoEntregaEnPuerta(UUID.randomUUID(), donacionId, LocalDateTime.now());
  }

  @Test
  void entregaEnPuerta_pasaLaDonacionEnTrasladoAEnPuertaYRegistraElEvento() {
    DonacionIndependiente enCamino =
        DonacionIndependienteMother.enTraslado(
            2, NecesidadMother.extraordinaria(UUID.randomUUID(), 10));
    EventoEntregaEnPuerta evento = enPuerta(enCamino.getId());
    when(eventosConsumidosRepository.yaFueConsumido(
            "EntregaEnPuerta", evento.entregaId(), enCamino.getId()))
        .thenReturn(false);
    when(donacionesIndependientesRepository.findById(enCamino.getId()))
        .thenReturn(Optional.of(enCamino));

    procesador.procesarEntregaEnPuerta(evento, ORIGEN);

    assertEquals(TipoEstadoDonacion.EN_PUERTA, enCamino.getEstadoActual().getTipo());
    verify(donacionesIndependientesRepository).save(enCamino);
    verify(eventosConsumidosRepository)
        .registrar(
            argThat(
                (EventoConsumido e) ->
                    "EntregaEnPuerta".equals(e.eventType())
                        && evento.entregaId().equals(e.businessId())));
  }

  @Test
  void entregaEnPuerta_siLaDonacionYaFueEntregada_noLaTocaPeroRegistraElEvento() {
    DonacionIndependiente entregada =
        DonacionIndependienteMother.entregada(
            2, NecesidadMother.extraordinaria(UUID.randomUUID(), 10));
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(entregada.getId()))
        .thenReturn(Optional.of(entregada));

    procesador.procesarEntregaEnPuerta(enPuerta(entregada.getId()), ORIGEN);

    assertEquals(TipoEstadoDonacion.ENTREGADA, entregada.getEstadoActual().getTipo());
    verify(donacionesIndependientesRepository, never()).save(any());
    verify(eventosConsumidosRepository).registrar(any(EventoConsumido.class));
  }

  @Test
  void entregaEnPuerta_duplicada_seIgnora() {
    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(true);

    procesador.procesarEntregaEnPuerta(enPuerta(UUID.randomUUID()), ORIGEN);

    verifyNoInteractions(donacionesIndependientesRepository);
    verify(eventosConsumidosRepository, never()).registrar(any());
  }
}
