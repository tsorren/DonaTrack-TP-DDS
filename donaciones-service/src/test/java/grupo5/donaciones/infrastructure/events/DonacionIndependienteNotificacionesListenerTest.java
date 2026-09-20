package grupo5.donaciones.infrastructure.events;

import static org.mockito.Mockito.*;

import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionRecibida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoRutaIniciada;
import grupo5.donaciones.services.IDonacionesIndependientesNotificacionesService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DonacionIndependienteNotificacionesListenerTest {

  @Mock private IDonacionesIndependientesNotificacionesService notificacionesService;

  @InjectMocks private DonacionIndependienteNotificacionesListener listener;

  @Test
  void onEventoRutaIniciada_deberiaDelegar_alServicio() {
    EventoRutaIniciada event =
        new EventoRutaIniciada(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "http://mapa/ruta");
    listener.onEventoRutaIniciada(event);
    verify(notificacionesService, times(1)).procesarRutaIniciada(event);
  }

  @Test
  void onEventoDonacionRecibida_deberiaDelegar_alServicio() {
    EventoDonacionRecibida event =
        new EventoDonacionRecibida(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ABC-123");
    listener.onEventoDonacionRecibida(event);
    verify(notificacionesService, times(1)).procesarDonacionRecibida(event);
  }

  @Test
  void onEventoDonacionFallida_deberiaDelegar_alServicio() {
    EventoDonacionFallida event =
        new EventoDonacionFallida(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Dirección no existe", false);
    listener.onEventoDonacionFallida(event);
    verify(notificacionesService, times(1)).procesarDonacionFallida(event);
  }
}
