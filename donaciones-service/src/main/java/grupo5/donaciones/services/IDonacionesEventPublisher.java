package grupo5.donaciones.services;

import grupo5.donaciones.dto.comunicaciones.EventoDonacionAsignadaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionEnCaminoV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionEntregaFallidaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionRecibidaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionSegmentadaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionVencidaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteDadoDeBajaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteRegistradoV1;
import grupo5.donaciones.dto.comunicaciones.EventoPersonaSincronizadaV1;

/**
 * Puerto de salida hacia RabbitMQ para los eventos de dominio que publica donaciones-service.
 * Reemplaza, evento por evento, a los FeignClient síncronos actuales.
 */
public interface IDonacionesEventPublisher {

  void publicarDonanteRegistrado(EventoDonanteRegistradoV1 evento);

  void publicarDonanteDadoDeBaja(EventoDonanteDadoDeBajaV1 evento);

  void publicarDonacionAsignada(EventoDonacionAsignadaV1 evento);

  void publicarDonacionEnCamino(EventoDonacionEnCaminoV1 evento);

  void publicarDonacionRecibida(EventoDonacionRecibidaV1 evento);

  void publicarDonacionEntregaFallida(EventoDonacionEntregaFallidaV1 evento);

  void publicarDonacionVencida(EventoDonacionVencidaV1 evento);

  void publicarPersonaSincronizada(EventoPersonaSincronizadaV1 evento);

  void publicarDonacionSegmentada(EventoDonacionSegmentadaV1 evento);
}
