package grupo5.donaciones.dto.donacionesIndependientes;

import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Filtros opcionales de {@code GET /donaciones-independientes}; un campo nulo no filtra. {@code
 * entidadId} es la entidad beneficiaria asignada. {@code desde} y {@code hasta} acotan la fecha de
 * registro y son inclusivos. Sin autenticación, el donante y la entidad llegan por parámetro (la
 * pertenencia se controla en la etapa 2 de SPEC-06). {@code necesidadId}: las que cubren esa
 * necesidad, en cualquier período (HB-18).
 */
public record FiltroDonacionesIndependientes(
    TipoEstadoDonacion estado,
    UUID subcategoriaId,
    UUID categoriaId,
    UUID donanteId,
    UUID entidadId,
    @DateTimeFormat(iso = ISO.DATE) LocalDate desde,
    @DateTimeFormat(iso = ISO.DATE) LocalDate hasta,
    UUID necesidadId) {}
