package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.SituacionPropuesta;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filtros opcionales de {@code GET /api/asignaciones/propuestas} (R11-24); un campo nulo o vacío no
 * filtra. {@code estado} y {@code situacion} se repiten; {@code situacion} solo deja pendientes.
 * {@code algoritmo}: {@code COMPATIBILIDAD_SEMANTICA}, {@code PRIORIDAD_SUBATENDIDOS} (solas o con
 * el otro) o {@code AMBOS} (las que sugirieron los dos). {@code donacion}: el id de una donación
 * independiente que piden. Un valor desconocido da 400 con {@code ERR-CSR-003}.
 *
 * <p>Desde R11-35 (aditivo), para Todas las decididas: {@code categoriaId}, la categoría de la
 * necesidad; {@code decididaPor}, el {@code sub} de quien decidió ({@code SISTEMA} junta las de
 * {@code SISTEMA} y las que no tienen autor guardado); {@code decididaDesde} y {@code
 * decididaHasta}, días inclusivos sobre la fecha de la decisión (las que no la tienen no entran).
 * La búsqueda es el {@code q} del listado.
 */
public record FiltroPropuestas(
    List<EstadoPropuesta> estado,
    List<SituacionPropuesta> situacion,
    String algoritmo,
    UUID donacion,
    UUID categoriaId,
    String decididaPor,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate decididaDesde,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate decididaHasta) {

  /** Sin los filtros de R11-35. */
  public FiltroPropuestas(
      List<EstadoPropuesta> estado,
      List<SituacionPropuesta> situacion,
      String algoritmo,
      UUID donacion) {
    this(estado, situacion, algoritmo, donacion, null, null, null, null);
  }

  public static FiltroPropuestas ninguno() {
    return new FiltroPropuestas(null, null, null, null);
  }
}
