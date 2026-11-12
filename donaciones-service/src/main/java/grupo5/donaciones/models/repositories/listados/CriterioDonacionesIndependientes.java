package grupo5.donaciones.models.repositories.listados;

import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Búsqueda de donaciones independientes (R11-01). Un campo nulo no filtra; {@code estados} vacío
 * tampoco. {@code donanteId}: las que salen de una donación de ese donante. {@code entidadId}: la
 * entidad asignada. {@code desde} y {@code hasta} acotan la fecha de registro y son inclusivos.
 * {@code q} busca en la descripción de sus bienes. {@code orden}: por fecha o, con {@code ATENCION}
 * (R11-43), por lo que hay que atender primero. {@code id}: solo esa donación (R11-43).
 */
public record CriterioDonacionesIndependientes(
    Set<TipoEstadoDonacion> estados,
    UUID subcategoriaId,
    UUID categoriaId,
    UUID donanteId,
    UUID entidadId,
    LocalDate desde,
    LocalDate hasta,
    UUID necesidadId,
    String q,
    OrdenDonacionesIndependientes orden,
    Paginado paginado,
    UUID id) {

  public CriterioDonacionesIndependientes {
    estados = estados == null ? Set.of() : Set.copyOf(estados);
  }

  /** Sin {@code id}: la búsqueda de antes de R11-43. */
  public CriterioDonacionesIndependientes(
      Set<TipoEstadoDonacion> estados,
      UUID subcategoriaId,
      UUID categoriaId,
      UUID donanteId,
      UUID entidadId,
      LocalDate desde,
      LocalDate hasta,
      UUID necesidadId,
      String q,
      OrdenDonacionesIndependientes orden,
      Paginado paginado) {
    this(
        estados,
        subcategoriaId,
        categoriaId,
        donanteId,
        entidadId,
        desde,
        hasta,
        necesidadId,
        q,
        orden,
        paginado,
        null);
  }
}
