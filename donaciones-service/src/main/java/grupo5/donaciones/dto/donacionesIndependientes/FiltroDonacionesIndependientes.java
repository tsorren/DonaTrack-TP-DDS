package grupo5.donaciones.dto.donacionesIndependientes;

import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Filtros opcionales de {@code GET /donaciones-independientes}; un campo nulo no filtra. {@code
 * estado} es repetible (R11-01): una o varias. {@code entidadId} es la entidad beneficiaria
 * asignada. {@code desde} y {@code hasta} acotan la fecha de registro y son inclusivos. Sin
 * autenticación, el donante y la entidad llegan por parámetro (la pertenencia se controla en la
 * etapa 2 de SPEC-06). {@code necesidadId}: las que cubren esa necesidad, en cualquier período
 * (HB-18). {@code id}: solo esa donación (R11-43), para que la entidad lea la de un acta sin
 * recorrer sus asignadas; combinado con {@code entidadId}, la de otra entidad no aparece.
 */
public record FiltroDonacionesIndependientes(
    List<TipoEstadoDonacion> estado,
    UUID subcategoriaId,
    UUID categoriaId,
    UUID donanteId,
    UUID entidadId,
    @DateTimeFormat(iso = ISO.DATE) LocalDate desde,
    @DateTimeFormat(iso = ISO.DATE) LocalDate hasta,
    UUID necesidadId,
    UUID id) {

  /** Sin {@code id}: los filtros de antes de R11-43. */
  public FiltroDonacionesIndependientes(
      List<TipoEstadoDonacion> estado,
      UUID subcategoriaId,
      UUID categoriaId,
      UUID donanteId,
      UUID entidadId,
      LocalDate desde,
      LocalDate hasta,
      UUID necesidadId) {
    this(
        estado, subcategoriaId, categoriaId, donanteId, entidadId, desde, hasta, necesidadId, null);
  }
}
