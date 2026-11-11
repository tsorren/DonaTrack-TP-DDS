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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * La bandeja de la entidad desde Notificaciones. La categoría sale del tipo de evento (HB-09); sin
 * tipo, "Otras novedades". Todas llevan a Donaciones recibidas. La que avisa que la entrega salió
 * lleva además el botón "Confirmar recepción" si Logística tiene esa donación en una entrega que
 * todavía espera y está entre las asignadas de la entidad, como lo exige Confirmar recepción (su
 * clave es la de la entrega, no la de la donación que trae el aviso); si Logística o Donaciones no
 * responden, queda el enlace simple.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.entidad", havingValue = "rest")
class EntidadNotificacionesRest implements EntidadNotificacionesPuerto {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final String OTRAS = "otras";
  private static final String EN_CAMINO = "DONACION_EN_CAMINO";
  private static final String A_RECIBIDAS = "/entidad-recibidas";

  /** Las categorías en el orden del filtro; el menú muestra solo las que tienen avisos. */
  private static final List<Tema> TEMAS =
      List.of(
          new Tema("camino", "Entregas en camino", "camion"),
          new Tema("asignadas", "Donaciones asignadas", "asigna"),
          new Tema("recibidas", "Donaciones recibidas", "tilde"),
          new Tema("problemas", "Problemas", "problema"),
          new Tema(OTRAS, "Otras novedades", "reloj"));

  /** La categoría de cada tipo de evento; los demás (y sin tipo) van a "otras". */
  private static final Map<String, String> TEMA_DE =
      Map.of(
          "DONACION_ASIGNADA",
          "asignadas",
          EN_CAMINO,
          "camino",
          "DONACION_RECIBIDA",
          "recibidas",
          "ENTREGA_FALLIDA",
          "problemas",
          "DONACION_VENCIDA",
          "problemas");

  /** El estado que Confirmar recepción acepta (EntidadConfirmarRest); si HB-22 suma otro, ambos. */
  private static final String ESPERANDO = "EN_TRASLADO";

  private final NotificacionesApi api;
  private final LogisticaApi logistica;
  private final DonacionesApi donaciones;
  private final Clock reloj;

  EntidadNotificacionesRest(
      NotificacionesApi api, LogisticaApi logistica, DonacionesApi donaciones, Clock reloj) {
    this.api = api;
    this.logistica = logistica;
    this.donaciones = donaciones;
    this.reloj = reloj;
  }

  @Override
  public EntidadNotificacionesVista bandeja(UUID entidad) {
    LocalDateTime ahora =
        LocalDateTime.now(reloj.withZone(ARGENTINA)).truncatedTo(ChronoUnit.MINUTES);
    List<NotificacionesApi.Notificacion> avisos =
        api.bandeja(entidad, null).stream()
            .filter(n -> n.id() != null && n.fechaCreacion() != null)
            .toList();
    Map<UUID, UUID> entregaDe = entregasEsperando(entidad, avisos);
    List<Novedad> novedades = avisos.stream().map(n -> novedad(n, entregaDe)).toList();
    Set<String> presentes = novedades.stream().map(Novedad::tema).collect(Collectors.toSet());
    List<Tema> temas = new ArrayList<>();
    temas.add(new Tema("", "Todas las categorías", null));
    TEMAS.stream().filter(t -> presentes.contains(t.valor())).forEach(temas::add);
    return new EntidadNotificacionesVista(
        novedades, temas, ahora.format(ISO), ahora.format(DIA_Y_ANIO) + ", " + ahora.format(HORA));
  }

  private static Novedad novedad(NotificacionesApi.Notificacion n, Map<UUID, UUID> entregaDe) {
    String tema = n.tipoEvento() == null ? OTRAS : TEMA_DE.getOrDefault(n.tipoEvento(), OTRAS);
    UUID entrega = n.referenciaId() == null ? null : entregaDe.get(n.referenciaId());
    boolean confirmable = EN_CAMINO.equals(n.tipoEvento()) && entrega != null;
    return new Novedad(
        n.id().toString(),
        tema,
        n.fechaCreacion().format(ISO),
        n.fechaLectura() != null,
        n.mensaje(),
        confirmable ? "Confirmar recepción" : null,
        confirmable ? n.mensaje() : null,
        confirmable ? "/entidad-seguimiento-confirmar?entrega=" + entrega : destino(tema),
        null);
  }

  private static String destino(String tema) {
    return switch (tema) {
      case "camino" -> A_RECIBIDAS + "?estado=en-camino";
      case "asignadas" -> A_RECIBIDAS + "?estado=por-salir";
      case "recibidas" -> A_RECIBIDAS + "?estado=recibidas";
      default -> A_RECIBIDAS;
    };
  }

  /**
   * De la donación del aviso a la entrega que la entidad todavía tiene que confirmar. Una llamada a
   * Logística y otra a Donaciones (las asignadas, como Confirmar recepción), y solo si hay un aviso
   * de entrega en camino con donación; sin respuesta, ninguna.
   */
  private Map<UUID, UUID> entregasEsperando(
      UUID entidad, List<NotificacionesApi.Notificacion> avisos) {
    if (avisos.stream()
        .noneMatch(n -> EN_CAMINO.equals(n.tipoEvento()) && n.referenciaId() != null)) {
      return Map.of();
    }
    try {
      List<LogisticaApi.Seguimiento> esperando =
          logistica.seguimientoDeEntidad(entidad).stream()
              .filter(s -> s.idEntrega() != null && s.idDonacion() != null)
              .filter(s -> ESPERANDO.equals(s.estadoEntrega()))
              .toList();
      if (esperando.isEmpty()) {
        return Map.of();
      }
      Set<UUID> asignadas =
          donaciones.asignadas(entidad).stream()
              .map(DonacionesApi.Donacion::id)
              .collect(Collectors.toSet());
      return esperando.stream()
          .filter(s -> asignadas.contains(s.idDonacion()))
          .collect(
              Collectors.toMap(
                  LogisticaApi.Seguimiento::idDonacion,
                  LogisticaApi.Seguimiento::idEntrega,
                  (a, b) -> a));
    } catch (RestClientException sinLogistica) {
      return Map.of();
    }
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
