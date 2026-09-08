package grupo5.donaciones.infrastructure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.infrastructure.idempotency.IEventosConsumidosRepository;
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
class LogisticaEventListenerIdempotenciaTest {

  @Mock private IDonacionesIndependientesService donacionesIndependientesService;
  @Mock private IEventosConsumidosRepository eventosConsumidosRepository;
  @Mock private IDonacionesIndependientesRepository donacionesIndependientesRepository;
  @InjectMocks private LogisticaEventListener listener;

  @Test
  void onRutaAsignada_dobleEntrega_noLanzaExcepcion() {
    UUID donacionId = UUID.randomUUID();
    EventoRutaAsignada evento =
        new EventoRutaAsignada(UUID.randomUUID(), donacionId, LocalDateTime.now());

    when(donacionesIndependientesService.cambiarEstado(
            eq(donacionId), any(), eq("logistica-service")))
        .thenReturn(mock(DonacionIndependienteResponseDTO.class))
        .thenThrow(new RuntimeException("ya está en LISTA_PARA_ENTREGAR"));
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> listener.onRutaAsignada(evento));
    assertDoesNotThrow(() -> listener.onRutaAsignada(evento));

    verify(donacionesIndependientesService, times(2))
        .cambiarEstado(eq(donacionId), any(), eq("logistica-service"));
  }

  @Test
  void onRutaIniciada_dobleEntregaConUnaDonacion_noLanzaExcepcion() {
    UUID donacionId = UUID.randomUUID();
    EventoRutaIniciada evento =
        new EventoRutaIniciada(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "AA-123-BB",
            List.of(donacionId),
            LocalDateTime.now(),
            "http://mapa/ruta");

    when(donacionesIndependientesService.cambiarEstado(
            eq(donacionId), any(), eq("logistica-service")))
        .thenReturn(mock(DonacionIndependienteResponseDTO.class))
        .thenThrow(new RuntimeException("ya está en EN_TRASLADO"));
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> listener.onRutaIniciada(evento));
    assertDoesNotThrow(() -> listener.onRutaIniciada(evento));

    verify(donacionesIndependientesService, times(2))
        .cambiarEstado(eq(donacionId), any(), eq("logistica-service"));
  }

  @Test
  void onEntregaExitosa_dobleEntrega_noLanzaExcepcion() {
    UUID donacionId = UUID.randomUUID();
    EventoEntregaExitosa evento =
        new EventoEntregaExitosa(
            UUID.randomUUID(), donacionId, UUID.randomUUID(), "AA-123-BB", LocalDateTime.now());

    when(donacionesIndependientesService.cambiarEstado(
            eq(donacionId), any(), eq("logistica-service")))
        .thenReturn(mock(DonacionIndependienteResponseDTO.class))
        .thenThrow(new RuntimeException("ya está en ENTREGADA"));
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> listener.onEntregaExitosa(evento));
    assertDoesNotThrow(() -> listener.onEntregaExitosa(evento));

    verify(donacionesIndependientesService, times(2))
        .cambiarEstado(eq(donacionId), any(), eq("logistica-service"));
  }

  @Test
  void onEntregaFallida_dobleEntrega_noLanzaExcepcion() {
    UUID donacionId = UUID.randomUUID();
    EventoEntregaFallida evento =
        new EventoEntregaFallida(
            UUID.randomUUID(), donacionId, "Dirección incorrecta", LocalDateTime.now(), false);

    when(donacionesIndependientesService.cambiarEstado(
            eq(donacionId), any(), eq("logistica-service")))
        .thenReturn(mock(DonacionIndependienteResponseDTO.class))
        .thenThrow(new RuntimeException("ya está en ENTREGA_FALLIDA"));
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> listener.onEntregaFallida(evento));
    assertDoesNotThrow(() -> listener.onEntregaFallida(evento));

    verify(donacionesIndependientesService, times(2))
        .cambiarEstado(eq(donacionId), any(), eq("logistica-service"));
  }

  @Test
  void onRutaAsignada_donacionInexistente_noLanzaExcepcion() {
    UUID donacionId = UUID.randomUUID();
    EventoRutaAsignada evento =
        new EventoRutaAsignada(UUID.randomUUID(), donacionId, LocalDateTime.now());

    when(donacionesIndependientesService.cambiarEstado(eq(donacionId), any(), any()))
        .thenThrow(new RecursoNoEncontradoException(donacionId));
    when(donacionesIndependientesRepository.findById(donacionId)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> listener.onRutaAsignada(evento));
  }
}
