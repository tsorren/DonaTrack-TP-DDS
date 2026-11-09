package grupo5.cliente.rest;

import grupo5.cliente.puertos.NotificacionesEntidadPuerto;
import grupo5.cliente.rest.NotificacionesApi.Notificacion;
import grupo5.cliente.vistas.NotificacionesEntidadVista;
import grupo5.cliente.vistas.NotificacionesEntidadVista.Novedad;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Novedades de la entidad desde {@code notificaciones-service}: pide las de la persona (entidad),
 * las ordena de la más reciente a la primera y las separa en sin leer y leídas.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.notificaciones-entidad", havingValue = "rest")
class NotificacionesEntidadRest implements NotificacionesEntidadPuerto {

  private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d 'de' MMMM", ES_AR);
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm", ES_AR);
  private static final DateTimeFormatter AHORA =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, H:mm", ES_AR);

  private final NotificacionesApi api;
  private final Clock reloj;

  NotificacionesEntidadRest(NotificacionesApi api, Clock reloj) {
    this.api = api;
    this.reloj = reloj;
  }

  @Override
  public NotificacionesEntidadVista novedades(UUID entidad) {
    LocalDateTime ahora = LocalDateTime.now(reloj.withZone(ARGENTINA));
    LocalDate hoy = ahora.toLocalDate();
    List<Notificacion> ordenadas =
        api.porPersona(entidad, null).stream()
            .sorted(Comparator.comparing(Notificacion::fechaCreacion).reversed())
            .toList();
    List<Novedad> sinLeer =
        ordenadas.stream().filter(n -> !n.leida()).map(n -> novedad(n, hoy)).toList();
    List<Novedad> leidas =
        ordenadas.stream().filter(Notificacion::leida).map(n -> novedad(n, hoy)).toList();
    return new NotificacionesEntidadVista(sinLeer, leidas, ahora.format(AHORA), sinLeer.size());
  }

  @Override
  public void marcarTodasLeidas(UUID entidad) {
    api.marcarTodasLeidas(entidad);
  }

  private static Novedad novedad(Notificacion n, LocalDate hoy) {
    LocalDateTime momento = n.fechaCreacion();
    return new Novedad(
        n.id().toString(),
        null,
        null,
        dia(momento.toLocalDate(), hoy),
        momento.format(HORA),
        momento.toString(),
        n.mensaje(),
        null,
        null,
        null,
        null,
        false,
        n.leida());
  }

  private static String dia(LocalDate fecha, LocalDate hoy) {
    if (fecha.equals(hoy)) {
      return "Hoy";
    }
    return fecha.equals(hoy.minusDays(1)) ? "Ayer" : fecha.format(DIA);
  }
}
