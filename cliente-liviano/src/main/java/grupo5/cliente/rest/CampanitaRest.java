package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;

import grupo5.cliente.puertos.CampanitaPuerto;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.vistas.CampanitaVista;
import grupo5.cliente.vistas.CampanitaVista.Fila;
import grupo5.cliente.vistas.Tramo;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

/**
 * La campanita desde Notificaciones (R11-29, R11-30; spec R11-p2-campanita §3.2): las 5 sin leer
 * más nuevas con {@code leida=false&size=5}, el total en {@code X-Total-Count}, y abrir una con el
 * {@code PATCH} de leída, que devuelve la notificación de la que sale el destino. La administración
 * usa la bandeja compartida de la Administración ({@code cliente.notificaciones.administracion}).
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.avisos", havingValue = "rest")
class CampanitaRest implements CampanitaPuerto {

  static final String ADMIN = "admin";

  /** Los títulos de los avisos de la administración (ficha, sección 6); los demás, "Novedad". */
  private static final Map<String, String> TITULOS_ADMIN =
      Map.of(
          "IMPORTACION_TERMINADA", "Terminó una importación",
          "IMPORTACION_CORTADA", "Una importación se cortó",
          "MENSAJE_CONTACTO", "Nuevo mensaje de contacto");

  private static final String NOVEDAD = "Novedad";
  private static final Pattern UUID_TEXTO =
      Pattern.compile(
          "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");

  private final NotificacionesApi api;
  private final CuentaActual cuentas;
  private final UUID administracion;
  private final Clock reloj;

  CampanitaRest(
      NotificacionesApi api,
      CuentaActual cuentas,
      @Value("${cliente.notificaciones.administracion:" + AvisosRest.ADMINISTRACION + "}")
          UUID administracion,
      Clock reloj) {
    this.api = api;
    this.cuentas = cuentas;
    this.administracion = administracion;
    this.reloj = reloj;
  }

  @Override
  public CampanitaVista recientes(String area, HttpServletRequest pedido) {
    String ahora =
        LocalDateTime.now(reloj.withZone(ARGENTINA)).truncatedTo(ChronoUnit.MINUTES).toString();
    Optional<UUID> persona = AvisosRest.persona(area, cuentas, administracion, pedido);
    if (persona.isEmpty()) {
      return new CampanitaVista(List.of(), 0, ahora);
    }
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("leida", "false");
    filtros.add("orden", "recientes");
    filtros.add("page", "0");
    filtros.add("size", String.valueOf(CampanitaVista.MAXIMO));
    Tramo<NotificacionesApi.Notificacion> pagina =
        Paginas.pedir(1, CampanitaVista.MAXIMO, p -> api.bandeja(persona.get(), filtros));
    List<Fila> filas =
        pagina.filas().stream()
            .filter(n -> n.id() != null && n.fechaCreacion() != null)
            .limit(CampanitaVista.MAXIMO)
            .map(
                n ->
                    new Fila(
                        n.id().toString(),
                        titulo(area, n.tipoEvento()),
                        n.fechaCreacion().truncatedTo(ChronoUnit.MINUTES).toString()))
            .toList();
    return new CampanitaVista(filas, Math.max(pagina.total(), filas.size()), ahora);
  }

  @Override
  public String abrir(String area, String id, HttpServletRequest pedido) {
    String bandeja = bandeja(area);
    if (id == null || !UUID_TEXTO.matcher(id).matches()) {
      return bandeja;
    }
    try {
      NotificacionesApi.Notificacion abierta = api.marcarLeida(UUID.fromString(id), true);
      return abierta == null ? bandeja : destino(area, abierta);
    } catch (RestClientException noSePudo) {
      // Ajena, inexistente o sin respuesta: la persona pidió ir a sus avisos y llega a la bandeja.
      return bandeja;
    }
  }

  static String bandeja(String area) {
    return "/" + area + "-notificaciones";
  }

  /** El título de la fila, el mismo que en la bandeja del área. */
  static String titulo(String area, String tipoEvento) {
    return switch (area) {
      case "donante" -> DonanteNotificacionesRest.titulo(tipoEvento);
      case "entidad" -> EntidadNotificacionesRest.titulo(tipoEvento);
      default -> tipoEvento == null ? NOVEDAD : TITULOS_ADMIN.getOrDefault(tipoEvento, NOVEDAD);
    };
  }

  /**
   * Adónde lleva el aviso (spec §3.3): el del donante y la organización, el de su bandeja; el de
   * importación, la página de esa importación (ella elige resultado, avance o error según el
   * estado). Sin tipo, sin referencia o el de contacto, la bandeja.
   */
  static String destino(String area, NotificacionesApi.Notificacion n) {
    if (n.referenciaId() == null) {
      return bandeja(area);
    }
    return switch (area) {
      case "donante" -> "/donante-donaciones-detalle?donacion=" + n.referenciaId();
      case "entidad" ->
          EntidadNotificacionesRest.destino(EntidadNotificacionesRest.tema(n.tipoEvento()));
      default ->
          n.tipoEvento() != null && n.tipoEvento().startsWith("IMPORTACION_")
              ? "/admin-donantes-importado?archivo=" + n.referenciaId()
              : bandeja(area);
    };
  }
}
