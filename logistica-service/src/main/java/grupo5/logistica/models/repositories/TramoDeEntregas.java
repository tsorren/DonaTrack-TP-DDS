package grupo5.logistica.models.repositories;

import grupo5.logistica.models.entities.entregas.Entrega;
import java.util.List;

/** Las entregas de una página y cuántas cumplen el criterio sin contar la página (R11-43). */
public record TramoDeEntregas(List<Entrega> entregas, long total) {

  public TramoDeEntregas {
    entregas = List.copyOf(entregas);
  }

  /** Corta una lista ya filtrada y ordenada (los repositorios que filtran en memoria). */
  public static TramoDeEntregas de(List<Entrega> ordenadas, CriterioSeguimiento criterio) {
    return new TramoDeEntregas(
        ordenadas.stream().skip(criterio.desplazamiento()).limit(criterio.tamanio()).toList(),
        ordenadas.size());
  }
}
