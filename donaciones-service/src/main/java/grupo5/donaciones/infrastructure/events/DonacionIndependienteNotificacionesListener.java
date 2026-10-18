package grupo5.donaciones.infrastructure.events;

import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionRecibida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionVencida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoRutaIniciada;
import grupo5.donaciones.services.IDonacionesIndependientesNotificacionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DonacionIndependienteNotificacionesListener {

  private final IDonacionesIndependientesNotificacionesService notificacionesService;

  @EventListener
  public void onEventoRutaIniciada(EventoRutaIniciada event) {
    notificacionesService.procesarRutaIniciada(event);
  }

  @EventListener
  public void onEventoDonacionRecibida(EventoDonacionRecibida event) {
    notificacionesService.procesarDonacionRecibida(event);
  }

  @EventListener
  public void onEventoDonacionVencida(EventoDonacionVencida event) {
    notificacionesService.procesarDonacionVencida(event);
  }

  @EventListener
  public void onEventoDonacionFallida(EventoDonacionFallida event) {
    notificacionesService.procesarDonacionFallida(event);
  }
}
