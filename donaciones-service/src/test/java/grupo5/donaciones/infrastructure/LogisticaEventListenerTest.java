package grupo5.donaciones.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogisticaEventListenerTest {

  @Mock private IDonacionesIndependientesService donacionesIndependientesService;

  private LogisticaEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new LogisticaEventListener(donacionesIndependientesService);
  }

  @Test
  void reentregaDelMismoEventoNoReprocesaLaTransicion() {
    UUID donacionId = UUID.randomUUID();
    EventoRutaAsignada evento =
        new EventoRutaAsignada(UUID.randomUUID(), donacionId, LocalDateTime.now());

    listener.onRutaAsignada(evento);
    listener.onRutaAsignada(evento);

    verify(donacionesIndependientesService, times(1))
        .cambiarEstado(
            eq(donacionId),
            any(CambioEstadoDonacionIndependienteRequestDTO.class),
            eq("logistica-service"));
    verifyNoMoreInteractions(donacionesIndependientesService);
  }

  @Test
  void unaFallaGenuinaPermiteReprocesarLaTransicion() {
    UUID donacionId = UUID.randomUUID();
    EventoRutaAsignada evento =
        new EventoRutaAsignada(UUID.randomUUID(), donacionId, LocalDateTime.now());

    org.mockito.Mockito.when(
            donacionesIndependientesService.cambiarEstado(
                eq(donacionId),
                any(CambioEstadoDonacionIndependienteRequestDTO.class),
                eq("logistica-service")))
        .thenThrow(new RuntimeException("falla transitoria"))
        .thenReturn(null);

    listener.onRutaAsignada(evento);
    listener.onRutaAsignada(evento);

    verify(donacionesIndependientesService, times(2))
        .cambiarEstado(
            eq(donacionId),
            any(CambioEstadoDonacionIndependienteRequestDTO.class),
            eq("logistica-service"));
  }

  @Test
  void eventosDeDonacionesDistintasSeProcesanIndependientemente() {
    UUID donacionUno = UUID.randomUUID();
    UUID donacionDos = UUID.randomUUID();

    listener.onRutaAsignada(
        new EventoRutaAsignada(UUID.randomUUID(), donacionUno, LocalDateTime.now()));
    listener.onRutaAsignada(
        new EventoRutaAsignada(UUID.randomUUID(), donacionDos, LocalDateTime.now()));

    verify(donacionesIndependientesService)
        .cambiarEstado(
            eq(donacionUno),
            eq(
                new CambioEstadoDonacionIndependienteRequestDTO(
                    TipoEstadoDonacion.LISTA_PARA_ENTREGAR, null, null, null, null, null)),
            eq("logistica-service"));
    verify(donacionesIndependientesService)
        .cambiarEstado(
            eq(donacionDos),
            eq(
                new CambioEstadoDonacionIndependienteRequestDTO(
                    TipoEstadoDonacion.LISTA_PARA_ENTREGAR, null, null, null, null, null)),
            eq("logistica-service"));
  }
}
