package grupo5.donaciones.models.repositories.listados;

import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
import java.time.LocalDate;
import java.util.Set;

/**
 * Búsqueda en el registro de actividad (R11-26). {@code autores} o {@code acciones} vacíos no
 * filtran; {@code desde} y {@code hasta} acotan el día de {@code ocurrio_en} (hora de Buenos Aires)
 * y son inclusivos. El orden es fijo: de la más reciente a la más vieja, desempata por id.
 */
public record CriterioActividad(
    Set<String> autores,
    Set<AccionDeAdministracion> acciones,
    LocalDate desde,
    LocalDate hasta,
    Paginado paginado) {

  public CriterioActividad {
    autores = autores == null ? Set.of() : Set.copyOf(autores);
    acciones = acciones == null ? Set.of() : Set.copyOf(acciones);
  }
}
