package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;
import static grupo5.cliente.rest.Comun.HORA;

import grupo5.cliente.puertos.EntidadNotificacionesPuerto;
import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.EntidadNotificacionesVista;
import grupo5.cliente.vistas.EntidadNotificacionesVista.Novedad;
import grupo5.cliente.vistas.EntidadNotificacionesVista.Tema;
import grupo5.cliente.vistas.Tramo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

/**
 * La bandeja de la entidad desde Notificaciones, de a una página (R11-14): la página, "Sin leer",
 * la búsqueda, la categoría y el orden los resuelve el servicio, y el total y las sin leer son sus
 * números. La categoría y el título salen del tipo de evento (HB-09); sin tipo, "Otras novedades" y
 * "Novedad". Todas llevan a Donaciones recibidas. La que avisa que la entrega salió lleva además el
 * botón "Confirmar recepción" si Logística tiene esa donación en una entrega que todavía espera y
 * está entre las asignadas de la entidad, como lo exige Confirmar recepción (su clave es la de la
 * entrega, no la de la donación que trae el aviso); si Logística o Donaciones no responden, queda
 * el vínculo simple. Eso se calcula solo para los avisos de la página.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.entidad", havingValue = "rest")
class EntidadNotificacionesRest implements EntidadNotificacionesPuerto {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final String EN_CAMINO = "DONACION_EN_CAMINO";
  private static final String A_RECIBIDAS = "/entidad-recibidas";
  private static final String NOVEDAD = "Novedad";

  /** Las categorías del menú, en su orden: siempre todas las del rol (ficha, sección 4). */
  static final List<Tema> TEMAS =
      List.of(
          new Tema("", "Todas las categorías", null),
          new Tema("camino", "Entregas en camino", "camion"),
          new Tema("asignadas", "Donaciones asignadas", "asigna"),
          new Tema("recibidas", "Donaciones recibidas", "tilde"),
          new Tema("problemas", "Problemas", "problema"),
          new Tema(Bandejas.OTRAS, "Otras novedades", "reloj"));

  /** Los tipos de evento de cada categoría; los demás (y sin tipo) van a "otras". */
  private static final Map<String, List<String>> TIPOS_DE = tiposDe();

  /** El título de cada tipo de aviso (ficha, sección 6); los demás y sin tipo, "Novedad". */
  private static final Map<String, String> TITULOS =
      Map.of(
          "DONACION_ASIGNADA",
          "Nueva donación asignada",
          EN_CAMINO,
          "Entrega en camino",
          "DONACION_RECIBIDA",
          "Entrega recibida",
          "ENTREGA_FALLIDA",
          "La entrega tuvo un problema",
          "DONACION_VENCIDA",
          "Donación vencida");

  /** Los estados que Confirmar recepción acepta (EntidadConfirmarRest): la puerta y el traslado. */
  private static final List<String> ESPERANDO = List.of("EN_PUERTA", "EN_TRASLADO");

  private final NotificacionesApi api;
  private final LogisticaApi logistica;
  private final Clock reloj;

  EntidadNotificacionesRest(NotificacionesApi api, LogisticaApi logistica, Clock reloj) {
    this.api = api;
    this.logistica = logistica;
    this.reloj = reloj;
  }

  private static Map<String, List<String>> tiposDe() {
    Map<String, List<String>> tipos = new LinkedHashMap<>();
    tipos.put("camino", List.of(EN_CAMINO));
    tipos.put("asignadas", List.of("DONACION_ASIGNADA"));
    tipos.put("recibidas", List.of("DONACION_RECIBIDA"));
    tipos.put("problemas", List.of("ENTREGA_FALLIDA", "DONACION_VENCIDA"));
    return Collections.unmodifiableMap(tipos);
  }

  @Override
  public EntidadNotificacionesVista bandeja(UUID entidad, ConsultaBandeja consulta) {
    LocalDateTime ahora =
        LocalDateTime.now(reloj.withZone(ARGENTINA)).truncatedTo(ChronoUnit.MINUTES);
    Tramo<NotificacionesApi.Notificacion> pagina =
        Bandejas.pagina(api, entidad, consulta, TIPOS_DE);
    List<NotificacionesApi.Notificacion> avisos =
        pagina.filas().stream().filter(n -> n.id() != null && n.fechaCreacion() != null).toList();
    Map<UUID, UUID> entregaDe = entregasEsperando(entidad, avisos);
    List<Novedad> novedades = avisos.stream().map(n -> novedad(n, entregaDe)).toList();
    return new EntidadNotificacionesVista(
        new Tramo<>(novedades, pagina.total(), pagina.pagina(), pagina.paginas()),
        Bandejas.sinLeer(api, entidad),
        TEMAS,
        ahora.format(ISO),
        ahora.format(DIA_Y_ANIO) + ", " + ahora.format(HORA));
  }

  private static Novedad novedad(NotificacionesApi.Notificacion n, Map<UUID, UUID> entregaDe) {
    String tema = tema(n.tipoEvento());
    UUID entrega = n.referenciaId() == null ? null : entregaDe.get(n.referenciaId());
    boolean confirmable = EN_CAMINO.equals(n.tipoEvento()) && entrega != null;
    String titulo = titulo(n.tipoEvento());
    return new Novedad(
        n.id().toString(),
        tema,
        n.fechaCreacion().format(ISO),
        n.fechaLectura() != null,
        titulo,
        n.mensaje() == null ? "" : n.mensaje(),
        "Ver en Donaciones recibidas",
        confirmable ? "Confirmar recepción" : null,
        confirmable ? n.mensaje() : null,
        confirmable ? "/entidad-seguimiento-confirmar?entrega=" + entrega : destino(tema),
        null);
  }

  /** El título del aviso según su tipo; sin tipo o de otro, "Novedad" (también en la campanita). */
  static String titulo(String tipoEvento) {
    // Map.of no admite buscar con null: sin tipo, "Novedad".
    return tipoEvento == null ? NOVEDAD : TITULOS.getOrDefault(tipoEvento, NOVEDAD);
  }

  static String tema(String tipoEvento) {
    // List.of no admite buscar con null: sin tipo, "Otras novedades".
    if (tipoEvento == null) {
      return Bandejas.OTRAS;
    }
    return TIPOS_DE.entrySet().stream()
        .filter(categoria -> categoria.getValue().contains(tipoEvento))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(Bandejas.OTRAS);
  }

  static String destino(String tema) {
    return switch (tema) {
      case "camino" -> A_RECIBIDAS + "?estado=en-camino";
      case "asignadas" -> A_RECIBIDAS + "?estado=por-salir";
      case "recibidas" -> A_RECIBIDAS + "?estado=recibidas";
      default -> A_RECIBIDAS;
    };
  }

  /**
   * De la donación del aviso a la entrega que la entidad todavía tiene que confirmar. Una sola
   * llamada a Logística, con las donaciones de los avisos de entrega en camino de la página ({@code
   * idDonacion}, R11-43): Logística ya da solo las entregas de esta entidad, así que no hace falta
   * leer sus asignadas. Sin avisos así, ninguna llamada; sin respuesta, ninguna entrega.
   */
  private Map<UUID, UUID> entregasEsperando(
      UUID entidad, List<NotificacionesApi.Notificacion> avisos) {
    List<String> donaciones =
        avisos.stream()
            .filter(n -> EN_CAMINO.equals(n.tipoEvento()) && n.referenciaId() != null)
            .map(n -> n.referenciaId().toString())
            .distinct()
            .toList();
    if (donaciones.isEmpty()) {
      return Map.of();
    }
    MultiValueMap<String, String> filtros = EntidadSeguimientoRest.deLasDonaciones(donaciones);
    ESPERANDO.forEach(e -> filtros.add("estado", e));
    try {
      return logistica.seguimientoDeEntidad(entidad, filtros).stream()
          .filter(s -> s.idEntrega() != null && s.idDonacion() != null && s.estadoEntrega() != null)
          .filter(s -> ESPERANDO.contains(s.estadoEntrega()))
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
      ids.stream().distinct().forEach(id -> Bandejas.marcar(api, id, true));
    }
  }

  @Override
  public void dejarSinLeer(UUID id) {
    Bandejas.marcar(api, id, false);
  }
}
