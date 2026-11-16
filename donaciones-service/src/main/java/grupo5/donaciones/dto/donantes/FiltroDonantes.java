package grupo5.donaciones.dto.donantes;

import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Filtros opcionales de {@code GET /api/donantes} (R11-01); un campo nulo no filtra. {@code canal}
 * (CORREO, TELEFONO o WHATSAPP) deja solo a los activos, no anonimizados y con ese medio. {@code
 * tipoJuridico} implica jurídicas. {@code categoriaId}: donó alguna vez algo de esa categoría.
 * {@code altaDesde} y {@code altaHasta} (R11-34) acotan el día de alta como donante y son
 * inclusivos.
 */
public record FiltroDonantes(
    String canal,
    TipoPersona tipo,
    TipoJuridico tipoJuridico,
    Boolean activo,
    UUID categoriaId,
    @DateTimeFormat(iso = ISO.DATE) LocalDate altaDesde,
    @DateTimeFormat(iso = ISO.DATE) LocalDate altaHasta) {

  /** Sin el período de alta: los filtros de R11-01. */
  public FiltroDonantes(
      String canal, TipoPersona tipo, TipoJuridico tipoJuridico, Boolean activo, UUID categoriaId) {
    this(canal, tipo, tipoJuridico, activo, categoriaId, null, null);
  }

  public static FiltroDonantes ninguno() {
    return new FiltroDonantes(null, null, null, null, null);
  }
}
