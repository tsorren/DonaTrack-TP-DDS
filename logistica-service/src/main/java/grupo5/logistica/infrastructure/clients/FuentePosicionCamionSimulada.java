package grupo5.logistica.infrastructure.clients;

import grupo5.logistica.models.entities.camiones.PosicionCamion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.services.FuentePosicionCamion;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
 *
 * <p>Una parada está alcanzada cuando el camión llegó a ella: su índice es menor o igual al del
 * tramo en curso (HB-22). En el paso 0 no hay ninguna: el camión recién sale, y así {@code
 * ruta.iniciada} le gana a la llegada en Donaciones, que solo la aplica sobre una donación en
 * traslado.
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
    if (!enTraslado(ruta)) {
      return Optional.empty();
    }
    List<CoordenadasGeograficas> paradas =
        paradasConCoordenadas(ruta).stream().map(e -> e.getDestino().coordenadas()).toList();
    if (paradas.isEmpty()) {
      return Optional.empty();
    }

    Instant inicio = ruta.getHoraInicioReal().toInstant(ZoneOffset.UTC);
    long paso = paso(ruta, paradas.size());

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

  @Override
  public List<UUID> entregasAlcanzadas(Ruta ruta) {
    if (!enTraslado(ruta)) {
      return List.of();
    }
    List<Entrega> paradas = paradasConCoordenadas(ruta);
    if (paradas.isEmpty()) {
      return List.of();
    }
    // Sin acotar al final: una ruta de una sola parada también la alcanza
    long paso = pasosTranscurridos(ruta);
    if (paso <= 0) {
      return List.of();
    }
    // Pasado el primer intervalo, la parada i se alcanza en el paso i * pasosPorTramo
    int alcanzadas = (int) Math.min(paso / pasosPorTramo + 1, paradas.size());
    return paradas.subList(0, alcanzadas).stream().map(Entrega::getId).toList();
  }

  private static boolean enTraslado(Ruta ruta) {
    return ruta != null
        && ruta.getEstado() == EstadoRuta.EN_TRASLADO
        && ruta.getHoraInicioReal() != null;
  }

  /** Sub-paso en curso desde el inicio real, acotado al final del recorrido. */
  private long paso(Ruta ruta, int cantidadParadas) {
    long pasoTotal = (long) (cantidadParadas - 1) * pasosPorTramo;
    return Math.clamp(pasosTranscurridos(ruta), 0L, pasoTotal);
  }

  private long pasosTranscurridos(Ruta ruta) {
    Instant inicio = ruta.getHoraInicioReal().toInstant(ZoneOffset.UTC);
    return Duration.between(inicio, clock.instant()).toMillis() / intervalo.toMillis();
  }

  /** Las entregas de la ruta en el orden de visita, sin las que no tienen coordenadas. */
  private List<Entrega> paradasConCoordenadas(Ruta ruta) {
    return ruta.getEntregaIds().stream()
        .map(entregasRepository::findById)
        .flatMap(Optional::stream)
        .filter(e -> e.getDestino() != null && e.getDestino().tieneCoordenadas())
        .toList();
  }
}
