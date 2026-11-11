package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;

import grupo5.cliente.puertos.DonanteNotificacionesPuerto;
import grupo5.cliente.vistas.DonanteNotificacionesVista;
import grupo5.cliente.vistas.DonanteNotificacionesVista.Aviso;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Las notificaciones del donante desde Notificaciones. La categoría sale del tipo de evento y el
 * mensaje lleva al detalle de la donación a la que se refiere (HB-09); sin tipo, "Otras novedades",
 * y sin donación, a Mis donaciones. Marcar solo toca las de la bandeja de la persona:
 * Notificaciones todavía no valida el token (HB-13).
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.donante", havingValue = "rest")
class DonanteNotificacionesRest implements DonanteNotificacionesPuerto {

  /** La categoría de cada tipo de evento; los demás (y sin tipo) van a "otras". */
  private static final Map<String, String> TEMAS =
      Map.of(
          "DONACION_ASIGNADA", "asignadas",
          "DONACION_EN_CAMINO", "camino",
          "DONACION_RECIBIDA", "entregadas",
          "ENTREGA_FALLIDA", "problemas",
          "DONACION_VENCIDA", "problemas");

  private static final String OTRAS = "otras";
  private static final String MIS_DONACIONES = "/donante-donaciones";

  private final NotificacionesApi api;
  private final Clock reloj;

  DonanteNotificacionesRest(NotificacionesApi api, Clock reloj) {
    this.api = api;
    this.reloj = reloj;
  }

  @Override
  public DonanteNotificacionesVista bandeja(UUID persona) {
    List<Aviso> avisos =
        api.bandeja(persona, null).stream()
            .filter(n -> n.id() != null && n.fechaCreacion() != null)
            .sorted(Comparator.comparing(NotificacionesApi.Notificacion::fechaCreacion).reversed())
            .map(DonanteNotificacionesRest::aviso)
            .toList();
    return new DonanteNotificacionesVista(
        avisos, iso(LocalDateTime.now(reloj.withZone(ARGENTINA))));
  }

  @Override
  public void marcar(UUID persona, List<String> ids, boolean leida) {
    Set<String> propias =
        api.bandeja(persona, null).stream()
            .map(NotificacionesApi.Notificacion::id)
            .filter(Objects::nonNull)
            .map(UUID::toString)
            .collect(Collectors.toSet());
    ids.stream()
        .filter(propias::contains)
        .distinct()
        .forEach(id -> api.marcarLeida(UUID.fromString(id), leida));
  }

  @Override
  public void marcarTodas(UUID persona) {
    api.marcarTodasLeidas(persona);
  }

  private static Aviso aviso(NotificacionesApi.Notificacion n) {
    String texto = n.mensaje() == null ? "" : n.mensaje();
    return new Aviso(
        n.id().toString(),
        iso(n.fechaCreacion()),
        texto,
        tema(n.tipoEvento()),
        n.referenciaId() == null
            ? MIS_DONACIONES
            : "/donante-donaciones-detalle?donacion=" + n.referenciaId(),
        n.fechaLectura() != null);
  }

  static String tema(String tipoEvento) {
    // Map.of no admite buscar con null: sin tipo, "otras".
    return tipoEvento == null ? OTRAS : TEMAS.getOrDefault(tipoEvento, OTRAS);
  }

  /** ISO a los minutos, como lo compara el buzón: "2026-10-15T16:25". */
  private static String iso(LocalDateTime momento) {
    return momento.truncatedTo(ChronoUnit.MINUTES).toString();
  }
}
