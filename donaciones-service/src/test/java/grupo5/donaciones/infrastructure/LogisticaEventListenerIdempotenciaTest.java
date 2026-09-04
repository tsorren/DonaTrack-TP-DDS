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
import grupo5.donaciones.services.IDonacionesIndependientesService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogisticaEventListenerIdempotenciaTest {

  @Mock private IDonacionesIndependientesService donacionesIndependientesService;
  @InjectMocks private LogisticaEventListener listener;

  @Test
  void onRutaAsignada_dobleEntrega_noLanzaExcepcion() {
    UUID donacionId = UUID.randomUUID();
    EventoRutaAsignada evento =
        new EventoRutaAsignada(UUID.randomUUID(), donacionId, LocalDateTime.now());

    when(donacionesIndependientesService.cambiarEstado(eq(donacionId), any(), eq("logistica-service")))
        .thenReturn(mock(DonacionIndependienteResponseDTO.class))
        .thenThrow(new RuntimeException("ya está en LISTA_PARA_ENTREGAR"));

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

    when(donacionesIndependientesService.cambiarEstado(eq(donacionId), any(), eq("logistica-service")))
        .thenReturn(mock(DonacionIndependienteResponseDTO.class))
        .thenThrow(new RuntimeException("ya está en EN_TRASLADO"));

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

    when(donacionesIndependientesService.cambiarEstado(eq(donacionId), any(), eq("logistica-service")))
        .thenReturn(mock(DonacionIndependienteResponseDTO.class))
        .thenThrow(new RuntimeException("ya está en ENTREGADA"));

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

    when(donacionesIndependientesService.cambiarEstado(eq(donacionId), any(), eq("logistica-service")))
        .thenReturn(mock(DonacionIndependienteResponseDTO.class))
        .thenThrow(new RuntimeException("ya está en ENTREGA_FALLIDA"));

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

    assertDoesNotThrow(() -> listener.onRutaAsignada(evento));
  }
}
