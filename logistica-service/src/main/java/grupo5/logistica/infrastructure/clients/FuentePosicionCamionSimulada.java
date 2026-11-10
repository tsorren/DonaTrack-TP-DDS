package grupo5.logistica.infrastructure.clients;

import grupo5.logistica.models.entities.camiones.PosicionCamion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.services.FuentePosicionCamion;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simula la posición del camión (ADR-E6-07, F1): recorre en línea recta las coordenadas de las
 * paradas en el orden de visita, un sub-paso por intervalo desde el inicio real de la ruta.
 *
 * <p>La posición se calcula a partir del tiempo transcurrido, sin estado propio: un tick repetido o
 * perdido no desfasa al camión, y {@code registradaEn} es la hora del sub-paso, así que repetir un
 * tick da la misma posición y {@code Camion#registrarPosicion} la descarta. Las paradas sin
 * coordenadas se saltean. El camión arranca en la primera parada y queda quieto en la última.
 */
@Component
public class FuentePosicionCamionSimulada implements FuentePosicionCamion {

  private final IEntregasRepository entregasRepository;
  private final Clock clock;
  private final Duration intervalo;
  private final int pasosPorTramo;

  public FuentePosicionCamionSimulada(
      IEntregasRepository entregasRepository,
      Clock clock,
      @Value("${logistica.posicion.intervalo-ms:30000}") long intervaloMs,
      @Value("${logistica.posicion.pasos-por-tramo:10}") int pasosPorTramo) {
    if (intervaloMs <= 0 || pasosPorTramo <= 0) {
      throw new IllegalArgumentException("intervalo-ms y pasos-por-tramo deben ser positivos");
    }
    this.entregasRepository = entregasRepository;
    this.clock = clock;
    this.intervalo = Duration.ofMillis(intervaloMs);
    this.pasosPorTramo = pasosPorTramo;
  }

  @Override
  public Optional<PosicionCamion> posicionActual(Ruta ruta) {
    if (ruta == null
        || ruta.getEstado() != EstadoRuta.EN_TRASLADO
        || ruta.getHoraInicioReal() == null) {
      return Optional.empty();
    }
    List<CoordenadasGeograficas> paradas = coordenadasDeParadas(ruta);
    if (paradas.isEmpty()) {
      return Optional.empty();
    }

    Instant inicio = ruta.getHoraInicioReal().toInstant(ZoneOffset.UTC);
    long transcurridos = Duration.between(inicio, clock.instant()).toMillis();
    long pasoTotal = (long) (paradas.size() - 1) * pasosPorTramo;
    long paso = Math.clamp(transcurridos / intervalo.toMillis(), 0L, pasoTotal);

    int tramo = (int) (paso / pasosPorTramo);
    CoordenadasGeograficas desde = paradas.get(tramo);
    CoordenadasGeograficas hasta = paradas.get(Math.min(tramo + 1, paradas.size() - 1));
    double fraccion = (double) (paso % pasosPorTramo) / pasosPorTramo;

    return Optional.of(
        new PosicionCamion(
            desde.latitud() + (hasta.latitud() - desde.latitud()) * fraccion,
            desde.longitud() + (hasta.longitud() - desde.longitud()) * fraccion,
            inicio.plus(intervalo.multipliedBy(paso))));
  }

  private List<CoordenadasGeograficas> coordenadasDeParadas(Ruta ruta) {
    return ruta.getEntregaIds().stream()
        .map(entregasRepository::findById)
        .flatMap(Optional::stream)
        .map(Entrega::getDestino)
        .filter(Objects::nonNull)
        .filter(Direccion::tieneCoordenadas)
        .map(Direccion::coordenadas)
        .toList();
  }
}
