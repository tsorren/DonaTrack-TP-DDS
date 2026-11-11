package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;

import grupo5.cliente.puertos.EntidadSeguimientoPuerto;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Las noticias de las entregas de la entidad desde Logística ({@code GET
 * /api/entregas/seguimiento/beneficiarios/{id}}): la última actualización de cada una, por la
 * donación que lleva. La posición del camión no se dibuja todavía (el dibujo es fijo; MapLibre,
 * D21).
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
  public Map<String, String> noticias(UUID entidad) {
    return api.seguimientoDeEntidad(entidad).stream()
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
}
