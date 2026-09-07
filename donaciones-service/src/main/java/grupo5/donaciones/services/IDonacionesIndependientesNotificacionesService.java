package grupo5.donaciones.services;

import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionRecibida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionVencida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoRutaIniciada;

public interface IDonacionesIndependientesNotificacionesService {
  void procesarRutaIniciada(EventoRutaIniciada event);

  void procesarDonacionRecibida(EventoDonacionRecibida event);

  void procesarDonacionVencida(EventoDonacionVencida event);

  void procesarDonacionFallida(EventoDonacionFallida event);
}
