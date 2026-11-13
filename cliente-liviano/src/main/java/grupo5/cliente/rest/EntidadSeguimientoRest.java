package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;

import grupo5.cliente.puertos.EntidadSeguimientoPuerto;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Las noticias de las entregas de la entidad desde Logística ({@code GET
 * /api/entregas/seguimiento/beneficiarios/{id}}): la última actualización de cada una, por la
 * donación que lleva. Sin el dibujo del ejemplo: la posición del camión no se dibuja todavía
 * (MapLibre, D21). Desde R11-43 pide solo las entregas de las donaciones de la página ({@code
 * idDonacion}, repetido; como mucho una página de 100): no lee todas las de la entidad.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.entidad", havingValue = "rest")
class EntidadSeguimientoRest implements EntidadSeguimientoPuerto {

  private final LogisticaApi api;

  EntidadSeguimientoRest(LogisticaApi api) {
    this.api = api;
  }

  @Override
  public Map<String, String> noticias(UUID entidad, Collection<String> claves) {
    List<String> donaciones = claves.stream().filter(EntidadSeguimientoRest::esId).toList();
    if (donaciones.isEmpty()) {
      return Map.of();
    }
    return api.seguimientoDeEntidad(entidad, deLasDonaciones(donaciones)).stream()
        .filter(s -> s.idDonacion() != null && s.ultimaActualizacion() != null)
        .collect(
            Collectors.toMap(
                s -> s.idDonacion().toString(),
                s ->
                    "Última noticia: "
                        + s.ultimaActualizacion().atZone(ARGENTINA).format(DIA_Y_HORA)
                        + ".",
                (una, otra) -> una));
  }

  /** Los filtros de las entregas de {@code donaciones}, de a una página que las trae a todas. */
  static MultiValueMap<String, String> deLasDonaciones(Collection<String> donaciones) {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    donaciones.forEach(d -> filtros.add("idDonacion", d));
    filtros.add("size", String.valueOf(LogisticaApi.TOPE));
    return filtros;
  }

  /** Si la clave es el id de una donación (la maqueta usa nombres). */
  static boolean esId(String clave) {
    try {
      UUID.fromString(clave);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  public boolean dibujoDeEjemplo() {
    return false;
  }
}
