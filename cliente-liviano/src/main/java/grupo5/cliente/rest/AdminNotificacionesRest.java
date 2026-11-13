package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;
import static grupo5.cliente.rest.Comun.HORA;

import grupo5.cliente.puertos.AdminNotificacionesPuerto;
import grupo5.cliente.vistas.AdminNotificacionesVista;
import grupo5.cliente.vistas.AdminNotificacionesVista.Aviso;
import grupo5.cliente.vistas.AdminNotificacionesVista.Tema;
import grupo5.cliente.vistas.ConsultaBandeja;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * La bandeja de la Administración desde Notificaciones (R11-30, spec R11-p2-campanita §3.2 y §3.3):
 * la de la persona {@code cliente.notificaciones.administracion}, la misma que cuenta la campanita,
 * de a 20 con los filtros resueltos por el servicio. Las categorías del admin (ficha §4) son
 * Importaciones, Mensajes de contacto y Otras novedades (las viejas sin tipo y las de otro tipo).
 * El título y el destino son los de la campanita: el aviso de importación lleva a la página de esa
 * importación y el de contacto no tiene vínculo (no hay pantalla de mensajes).
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.avisos", havingValue = "rest")
class AdminNotificacionesRest implements AdminNotificacionesPuerto {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final String TERMINADA = "IMPORTACION_TERMINADA";
  private static final String CORTADA = "IMPORTACION_CORTADA";
  private static final String IMPORTACIONES = "importaciones";

  /** Las categorías del menú, en su orden: siempre todas las del rol (ficha, sección 4). */
  static final List<Tema> TEMAS =
      List.of(
          new Tema("", "Todas las categorías", null),
          new Tema(IMPORTACIONES, "Importaciones", "archivo"),
          new Tema("contacto", "Mensajes de contacto", "sobre"),
          new Tema(Bandejas.OTRAS, "Otras novedades", "reloj"));

  /** Los tipos de evento de cada categoría; los demás (y sin tipo) van a "otras". */
  private static final Map<String, List<String>> TIPOS_DE = tiposDe();

  private final NotificacionesApi api;
  private final UUID administracion;
  private final Clock reloj;

  AdminNotificacionesRest(
      NotificacionesApi api,
      @Value("${cliente.notificaciones.administracion:" + AvisosRest.ADMINISTRACION + "}")
          UUID administracion,
      Clock reloj) {
    this.api = api;
    this.administracion = administracion;
    this.reloj = reloj;
  }

  private static Map<String, List<String>> tiposDe() {
    Map<String, List<String>> tipos = new LinkedHashMap<>();
    tipos.put(IMPORTACIONES, List.of(TERMINADA, CORTADA));
    tipos.put("contacto", List.of("MENSAJE_CONTACTO"));
    return Collections.unmodifiableMap(tipos);
  }

  @Override
  public AdminNotificacionesVista bandeja(ConsultaBandeja consulta) {
    LocalDateTime ahora =
        LocalDateTime.now(reloj.withZone(ARGENTINA)).truncatedTo(ChronoUnit.MINUTES);
    Tramo<NotificacionesApi.Notificacion> pagina =
        Bandejas.pagina(api, administracion, consulta, TIPOS_DE);
    List<Aviso> avisos =
        pagina.filas().stream()
            .filter(n -> n.id() != null && n.fechaCreacion() != null)
            .map(AdminNotificacionesRest::aviso)
            .toList();
    return new AdminNotificacionesVista(
        new Tramo<>(avisos, pagina.total(), pagina.pagina(), pagina.paginas()),
        Bandejas.sinLeer(api, administracion),
        TEMAS,
        ahora.format(ISO),
        ahora.format(DIA_Y_ANIO) + ", " + ahora.format(HORA));
  }

  private static Aviso aviso(NotificacionesApi.Notificacion n) {
    String destino = CampanitaRest.destino(CampanitaRest.ADMIN, n);
    boolean conDestino = !CampanitaRest.bandeja(CampanitaRest.ADMIN).equals(destino);
    return new Aviso(
        n.id().toString(),
        tema(n.tipoEvento()),
        n.fechaCreacion().format(ISO),
        n.fechaLectura() != null,
        CampanitaRest.titulo(CampanitaRest.ADMIN, n.tipoEvento()),
        n.mensaje() == null ? "" : n.mensaje(),
        conDestino ? vinculo(n.tipoEvento()) : null,
        conDestino ? destino : null);
  }

  /** El texto del vínculo (ficha, sección 6): el resultado o qué pasó. */
  static String vinculo(String tipoEvento) {
    return CORTADA.equals(tipoEvento) ? "Ver qué pasó" : "Ver el resultado";
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

  @Override
  public void marcarLeidas(List<UUID> ids) {
    if (ids.isEmpty()) {
      api.marcarTodasLeidas(administracion);
    } else {
      ids.stream().distinct().forEach(id -> Bandejas.marcar(api, id, true));
    }
  }

  @Override
  public void dejarSinLeer(UUID id) {
    Bandejas.marcar(api, id, false);
  }
}
