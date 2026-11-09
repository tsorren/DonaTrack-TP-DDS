package grupo5.donaciones.infrastructure.logistica;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaCancelada;
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
}
