package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.EntidadSeguimientoPuerto;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Las noticias del aceite (no pudo llegar) y del arroz (en camino) del Comedor Barrio Ejemplo. */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.entidad",
    havingValue = "maqueta",
    matchIfMissing = true)
class EntidadSeguimientoMaqueta implements EntidadSeguimientoPuerto {

  private static final Map<String, String> NOTICIAS =
      Map.of(
          "aceite",
          "Última noticia: 14 de octubre, 16:40.",
          "arroz",
          "El camión va por la mitad del camino. Posición aproximada, actualizada hoy a las 18:25.");

  @Override
  public Map<String, String> noticias(UUID entidad, Collection<String> claves) {
    return NOTICIAS;
  }

  @Override
  public boolean dibujoDeEjemplo() {
    return true;
  }
}
