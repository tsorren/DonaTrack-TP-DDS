package grupo5.cliente.rest;

import grupo5.cliente.puertos.NotificacionesDonantePuerto;
import grupo5.cliente.rest.NotificacionesApi.Notificacion;
import grupo5.cliente.vistas.NotificacionesDonanteVista;
import grupo5.cliente.vistas.NotificacionesDonanteVista.Noticia;
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
 * Notificaciones del donante desde {@code notificaciones-service}: pide las de la persona, las
 * ordena de la más reciente a la primera y las separa en sin leer y leídas.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.notificaciones-donante", havingValue = "rest")
class NotificacionesDonanteRest implements NotificacionesDonantePuerto {

  private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final DateTimeFormatter DIA_Y_HORA =
      DateTimeFormatter.ofPattern("d 'de' MMMM, H:mm", ES_AR);
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm", ES_AR);

  private final NotificacionesApi api;
  private final Clock reloj;

  NotificacionesDonanteRest(NotificacionesApi api, Clock reloj) {
    this.api = api;
    this.reloj = reloj;
  }

  @Override
  public NotificacionesDonanteVista notificaciones(UUID donante) {
    LocalDate hoy = LocalDate.now(reloj.withZone(ARGENTINA));
    List<Notificacion> ordenadas =
        api.porPersona(donante, null).stream()
            .sorted(Comparator.comparing(Notificacion::fechaCreacion).reversed())
            .toList();
    List<Noticia> sinLeer =
        ordenadas.stream().filter(n -> !n.leida()).map(n -> noticia(n, hoy)).toList();
    List<Noticia> leidas =
        ordenadas.stream().filter(Notificacion::leida).map(n -> noticia(n, hoy)).toList();
    return new NotificacionesDonanteVista(sinLeer, leidas);
  }

  @Override
  public void marcarTodasLeidas(UUID donante) {
    api.marcarTodasLeidas(donante);
  }

  private static Noticia noticia(Notificacion n, LocalDate hoy) {
    LocalDateTime momento = n.fechaCreacion();
    return new Noticia(n.mensaje(), cuando(momento, hoy), momento.toString(), null, null, null);
  }

  private static String cuando(LocalDateTime momento, LocalDate hoy) {
    LocalDate dia = momento.toLocalDate();
    if (dia.equals(hoy)) {
      return "Hoy, " + momento.format(HORA);
    }
    return dia.equals(hoy.minusDays(1))
        ? "Ayer, " + momento.format(HORA)
        : momento.format(DIA_Y_HORA);
  }
}
