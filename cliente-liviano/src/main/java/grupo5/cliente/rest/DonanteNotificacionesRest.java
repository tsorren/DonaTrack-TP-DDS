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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Las notificaciones del donante desde Notificaciones. El DTO no dice de qué tipo es ni a qué
 * donación va (HB-09, fuera de esta corrida): la categoría se deduce del texto y el mensaje lleva a
 * Mis donaciones (o a Entidades, si es de una necesidad). Marcar solo toca las de la bandeja de la
 * persona: Notificaciones todavía no valida el token (HB-13).
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.donante", havingValue = "rest")
class DonanteNotificacionesRest implements DonanteNotificacionesPuerto {

  /** Qué parte del texto dice la categoría, en el orden en que se prueba. */
  // ponytail: deducido del texto; con el tipo en el DTO (HB-09), se lee de ahí.
  private static final List<Map.Entry<String, List<String>>> TEMAS =
      List.of(
          Map.entry("problemas", List.of("no pudo llegar", "no pudimos entregar")),
          Map.entry("entregadas", List.of("recibió tu donación", "confirmó que recibió")),
          Map.entry("camino", List.of("salió a entregar", "quedó lista")),
          Map.entry("asignadas", List.of("asignamos tu donación", "fue asignada")),
          Map.entry("necesidades", List.of("necesidad nueva", "publicó una necesidad")),
          Map.entry("deposito", List.of("recibimos tu donación")));

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
    String tema = tema(texto);
    return new Aviso(
        n.id().toString(),
        iso(n.fechaCreacion()),
        texto,
        tema,
        "necesidades".equals(tema) ? "/donante-entidades" : "/donante-donaciones",
        n.fechaLectura() != null);
  }

  static String tema(String texto) {
    String minusculas = texto.toLowerCase(Locale.ROOT);
    return TEMAS.stream()
        .filter(t -> t.getValue().stream().anyMatch(minusculas::contains))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse("otras");
  }

  /** ISO a los minutos, como lo compara el buzón: "2026-10-15T16:25". */
  private static String iso(LocalDateTime momento) {
    return momento.truncatedTo(ChronoUnit.MINUTES).toString();
  }
}
