package grupo5.donaciones.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.infrastructure.idempotency.EventoConsumido;
import grupo5.donaciones.infrastructure.idempotency.IEventosConsumidosRepository;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.EstadoDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogisticaEventListenerTest {

  @Mock private IDonacionesIndependientesService donacionesIndependientesService;
  @Mock private IEventosConsumidosRepository eventosConsumidosRepository;
  @Mock private IDonacionesIndependientesRepository donacionesIndependientesRepository;

  @InjectMocks private LogisticaEventListener listener;

  @Test
  void onRutaAsignada_cuandoEventoNuevo_aplicaCambioEstadoYRegistraComoConsumido() {
    UUID donacionId = UUID.randomUUID();
    EventoRutaAsignada evento =
        new EventoRutaAsignada(UUID.randomUUID(), donacionId, LocalDateTime.now());

    when(eventosConsumidosRepository.yaFueConsumido(any(), any(), any())).thenReturn(false);
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());

    listener.onRutaAsignada(evento);

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

    listener.onRutaAsignada(evento);

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

    listener.onEntregaExitosa(evento);

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

    listener.onRutaIniciada(evento);

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

    listener.onRutaIniciada(evento);

    verify(donacionesIndependientesService, times(1))
        .cambiarEstado(eq(donacionNuevaId), any(), any());
    verify(donacionesIndependientesService, never())
        .cambiarEstado(eq(donacionDuplicadaId), any(), any());
  }
}
