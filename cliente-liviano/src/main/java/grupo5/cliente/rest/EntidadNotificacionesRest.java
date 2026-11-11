package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;
import static grupo5.cliente.rest.Comun.HORA;

import grupo5.cliente.puertos.EntidadNotificacionesPuerto;
import grupo5.cliente.vistas.EntidadNotificacionesVista;
import grupo5.cliente.vistas.EntidadNotificacionesVista.Novedad;
import grupo5.cliente.vistas.EntidadNotificacionesVista.Tema;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * La bandeja de la entidad desde Notificaciones. {@code NotificacionDTO} no trae el tipo de evento
 * ni la donación (HB-09, fuera de alcance): todas van a la categoría "Novedades" y llevan a
 * Donaciones recibidas, sin el botón de confirmar.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.entidad", havingValue = "rest")
class EntidadNotificacionesRest implements EntidadNotificacionesPuerto {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final String NOVEDADES = "novedades";
  private static final List<Tema> TEMAS =
      List.of(
          new Tema("", "Todas las categorías", null), new Tema(NOVEDADES, "Novedades", "reloj"));

  private final NotificacionesApi api;
  private final Clock reloj;

  EntidadNotificacionesRest(NotificacionesApi api, Clock reloj) {
    this.api = api;
    this.reloj = reloj;
  }

  @Override
  public EntidadNotificacionesVista bandeja(UUID entidad) {
    LocalDateTime ahora =
        LocalDateTime.now(reloj.withZone(ARGENTINA)).truncatedTo(ChronoUnit.MINUTES);
    List<Novedad> novedades =
        api.bandeja(entidad, null).stream()
            .filter(n -> n.id() != null && n.fechaCreacion() != null)
            .map(
                n ->
                    new Novedad(
                        n.id().toString(),
                        NOVEDADES,
                        n.fechaCreacion().format(ISO),
                        n.fechaLectura() != null,
                        n.mensaje(),
                        null,
                        null,
                        "/entidad-recibidas",
                        null))
            .toList();
    return new EntidadNotificacionesVista(
        novedades, TEMAS, ahora.format(ISO), ahora.format(DIA_Y_ANIO) + ", " + ahora.format(HORA));
  }

  @Override
  public void marcarLeidas(UUID entidad, List<UUID> ids) {
    if (ids.isEmpty()) {
      api.marcarTodasLeidas(entidad);
    } else {
      ids.forEach(id -> api.marcarLeida(id, true));
    }
  }

  @Override
  public void dejarSinLeer(UUID id) {
    api.marcarLeida(id, false);
  }
}
