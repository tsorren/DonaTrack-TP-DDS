package grupo5.logistica.infrastructure;

import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.dto.eventos.EventoEntregaFallida;
import grupo5.logistica.dto.eventos.EventoRutaAsignada;
import grupo5.logistica.dto.eventos.EventoRutaIniciada;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.NoRecepcion;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.services.ComunicadorEventosLogistica;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ComunicadorEventosLogisticaRabbit implements ComunicadorEventosLogistica {

  private final LogisticaEventPublisher eventPublisher;
  private final GeneradorDeURLSeguimiento generadorDeUrlSeguimiento;

  public ComunicadorEventosLogisticaRabbit(
      LogisticaEventPublisher eventPublisher, GeneradorDeURLSeguimiento generadorDeUrlSeguimiento) {
    this.eventPublisher = eventPublisher;
    this.generadorDeUrlSeguimiento = generadorDeUrlSeguimiento;
  }

  @Override
  public void comunicarRutaAsignada(Ruta ruta, Entrega entrega) {
    eventPublisher.publicarRutaAsignada(
        new EventoRutaAsignada(ruta.getId(), entrega.getIdDonacion(), ahora()));
  }

  @Override
  public void comunicarRutaIniciada(Ruta ruta, Camion camion, List<Entrega> entregas) {
    List<UUID> donacionesIds = entregas.stream().map(Entrega::getIdDonacion).toList();
    eventPublisher.publicarRutaIniciada(
        new EventoRutaIniciada(
            ruta.getId(),
            camion.getId(),
            camion.getPatente(),
            donacionesIds,
            ruta.getHoraInicioReal(),
            generadorDeUrlSeguimiento.generarUrl(ruta.getId())));
  }

  @Override
  public void comunicarEntregaExitosa(Entrega entrega, Camion camion) {
    eventPublisher.publicarEntregaExitosa(
        new EventoEntregaExitosa(
            entrega.getId(),
            entrega.getIdDonacion(),
            camion.getId(),
            camion.getPatente(),
            ahora()));
  }

  @Override
  public void comunicarEntregaFallida(NoRecepcion solicitud) {
    Entrega entrega = solicitud.entrega();
    eventPublisher.publicarEntregaFallida(
        new EventoEntregaFallida(
            entrega.getId(),
            entrega.getIdDonacion(),
            solicitud.justificacion(),
            ahora(),
            solicitud.replanificable()));
  }

  private static LocalDateTime ahora() {
    return LocalDateTime.now(ZoneId.of("UTC"));
  }
}
