package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;

import grupo5.cliente.puertos.DonanteNotificacionesPuerto;
import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.DonanteNotificacionesVista;
import grupo5.cliente.vistas.DonanteNotificacionesVista.Aviso;
import grupo5.cliente.vistas.DonanteNotificacionesVista.Tema;
import grupo5.cliente.vistas.Tramo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Las notificaciones del donante desde Notificaciones, de a una página (R11-14): la página, "Sin
 * leer", la búsqueda, la categoría y el orden los resuelve el servicio, y el total y las sin leer
 * son sus números. La categoría y el título salen del tipo de evento; sin tipo, "Otras novedades" y
 * "Novedad". El vínculo lleva al detalle de la donación a la que se refiere (HB-09) o, sin
 * donación, a Mis donaciones. Quién es dueño de cada una al marcar lo controla Notificaciones.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.donante", havingValue = "rest")
class DonanteNotificacionesRest implements DonanteNotificacionesPuerto {

  /** Los tipos de evento de cada categoría, en el orden del menú; los demás van a "otras". */
  private static final Map<String, List<String>> TIPOS_DE = tiposDe();

  /** Las categorías del menú: siempre todas las del rol (ficha, sección 4). */
  static final List<Tema> TEMAS =
      List.of(
          new Tema("", "Todas las categorías", null),
          new Tema("camino", "Donaciones en camino", "camion"),
          new Tema("entregadas", "Donaciones entregadas", "tilde"),
          new Tema("asignadas", "Donaciones asignadas", "asigna"),
          new Tema("problemas", "Problemas en la entrega", "problema"),
          new Tema(Bandejas.OTRAS, "Otras novedades", "mision"));

  /** El título de cada tipo de aviso (ficha, sección 6); los demás y sin tipo, "Novedad". */
  private static final Map<String, String> TITULOS =
      Map.of(
          "DONACION_ASIGNADA", "Donación asignada",
          "DONACION_EN_CAMINO", "Donación en camino",
          "DONACION_RECIBIDA", "Donación entregada",
          "ENTREGA_FALLIDA", "La entrega tuvo un problema",
          "DONACION_VENCIDA", "Donación vencida",
          "MISION_CUMPLIDA", "Misión cumplida",
          "SUBIO_CATEGORIA", "Subiste de categoría",
          "DONANTE_REGISTRADO", "Te damos la bienvenida");

  private static final String NOVEDAD = "Novedad";
  private static final String MIS_DONACIONES = "/donante-donaciones";
  private static final Pattern UUID_TEXTO =
      Pattern.compile(
          "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");

  private final NotificacionesApi api;
  private final Clock reloj;

  DonanteNotificacionesRest(NotificacionesApi api, Clock reloj) {
    this.api = api;
    this.reloj = reloj;
  }

  private static Map<String, List<String>> tiposDe() {
    Map<String, List<String>> tipos = new LinkedHashMap<>();
    tipos.put("camino", List.of("DONACION_EN_CAMINO"));
    tipos.put("entregadas", List.of("DONACION_RECIBIDA"));
    tipos.put("asignadas", List.of("DONACION_ASIGNADA"));
    tipos.put("problemas", List.of("ENTREGA_FALLIDA", "DONACION_VENCIDA"));
    return Collections.unmodifiableMap(tipos);
  }

  @Override
  public DonanteNotificacionesVista bandeja(UUID persona, ConsultaBandeja consulta) {
    Tramo<NotificacionesApi.Notificacion> pagina =
        Bandejas.pagina(api, persona, consulta, TIPOS_DE);
    List<Aviso> avisos =
        pagina.filas().stream()
            .filter(n -> n.id() != null && n.fechaCreacion() != null)
            .map(DonanteNotificacionesRest::aviso)
            .toList();
    return new DonanteNotificacionesVista(
        new Tramo<>(avisos, pagina.total(), pagina.pagina(), pagina.paginas()),
        Bandejas.sinLeer(api, persona),
        TEMAS,
        iso(LocalDateTime.now(reloj.withZone(ARGENTINA))));
  }

  /** Marca de a una, sin pedir la bandeja: un id que no es de la persona se saltea. */
  @Override
  public void marcar(UUID persona, List<String> ids, boolean leida) {
    ids.stream()
        .filter(id -> id != null && UUID_TEXTO.matcher(id).matches())
        .distinct()
        .forEach(id -> Bandejas.marcar(api, UUID.fromString(id), leida));
  }

  @Override
  public void marcarTodas(UUID persona) {
    api.marcarTodasLeidas(persona);
  }

  private static Aviso aviso(NotificacionesApi.Notificacion n) {
    boolean conDonacion = n.referenciaId() != null;
    return new Aviso(
        n.id().toString(),
        iso(n.fechaCreacion()),
        titulo(n.tipoEvento()),
        n.mensaje() == null ? "" : n.mensaje(),
        tema(n.tipoEvento()),
        conDonacion ? "/donante-donaciones-detalle?donacion=" + n.referenciaId() : MIS_DONACIONES,
        conDonacion ? "Ver la donación" : "Ver Mis donaciones",
        n.fechaLectura() != null);
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

  static String titulo(String tipoEvento) {
    // Map.of no admite buscar con null: sin tipo, "Novedad".
    return tipoEvento == null ? NOVEDAD : TITULOS.getOrDefault(tipoEvento, NOVEDAD);
  }

  /** ISO a los minutos, como lo compara el buzón: "2026-10-15T16:25". */
  private static String iso(LocalDateTime momento) {
    return momento.truncatedTo(ChronoUnit.MINUTES).toString();
  }
}
