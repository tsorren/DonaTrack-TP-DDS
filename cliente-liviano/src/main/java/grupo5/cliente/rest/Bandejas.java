package grupo5.cliente.rest;

import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.Tramo;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Lo común de las bandejas del donante y de la entidad contra Notificaciones (R11-14, spec
 * R11-p1-notificaciones §3.4): pedir una página con los filtros de la dirección, el número de sin
 * leer de la cuenta y marcar de a una. La categoría de la pantalla se traduce a tipos de evento;
 * "otras" son todas menos las de las demás categorías del rol (también las que no tienen tipo).
 */
final class Bandejas {

  static final String OTRAS = "otras";

  private Bandejas() {}

  /**
   * Una página de la bandeja de {@code persona}. {@code tiposDe}: los tipos de cada categoría del
   * rol; una categoría que no está ahí (ni es "otras") se pide sin filtro de categoría.
   */
  static Tramo<NotificacionesApi.Notificacion> pagina(
      NotificacionesApi api,
      UUID persona,
      ConsultaBandeja consulta,
      Map<String, List<String>> tiposDe) {
    return Paginas.pedir(
        consulta.pagina(),
        ConsultaBandeja.TAMANIO,
        pagina -> api.bandeja(persona, filtros(consulta, tiposDe, pagina)));
  }

  /** Los parámetros del pedido, en el orden del contrato; {@code pagina} desde 0. */
  static MultiValueMap<String, String> filtros(
      ConsultaBandeja consulta, Map<String, List<String>> tiposDe, int pagina) {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    if (consulta.sinLeer()) {
      filtros.add("leida", "false");
    }
    if (!consulta.buscar().isEmpty()) {
      filtros.add("q", consulta.buscar());
    }
    if (OTRAS.equals(consulta.tema())) {
      tiposDe.values().stream()
          .flatMap(Collection::stream)
          .forEach(tipo -> filtros.add("excluirTipo", tipo));
    } else {
      tiposDe.getOrDefault(consulta.tema(), List.of()).forEach(tipo -> filtros.add("tipo", tipo));
    }
    if (consulta.viejas()) {
      filtros.add("orden", "antiguas");
    }
    filtros.add("page", String.valueOf(pagina));
    filtros.add("size", String.valueOf(ConsultaBandeja.TAMANIO));
    return filtros;
  }

  /** Cuántas sin leer tiene la cuenta, del conteo del servicio; 0 si no contesta. */
  static long sinLeer(NotificacionesApi api, UUID persona) {
    try {
      NotificacionesApi.Conteo conteo = api.sinLeer(persona);
      return conteo == null || conteo.noLeidas() == null ? 0 : conteo.noLeidas();
    } catch (RestClientException sinConteo) {
      return 0;
    }
  }

  /**
   * Marca una como leída o no leída. Quién es dueño lo controla Notificaciones ({@code
   * MARCAR_SU_NOTIFICACION}): una ajena (403) o que no existe (404) se saltea.
   */
  static void marcar(NotificacionesApi api, UUID id, boolean leida) {
    try {
      api.marcarLeida(id, leida);
    } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.NotFound ajena) {
      // No es de la persona o ya no existe: sigue con las demás.
    }
  }
}
