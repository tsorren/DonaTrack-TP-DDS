package grupo5.donaciones.dto.donantes;

import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.UUID;

/**
 * Filtros opcionales de {@code GET /api/donantes} (R11-01); un campo nulo no filtra. {@code canal}
 * (CORREO, TELEFONO o WHATSAPP) deja solo a los activos, no anonimizados y con ese medio. {@code
 * tipoJuridico} implica jurídicas. {@code categoriaId}: donó alguna vez algo de esa categoría.
 */
public record FiltroDonantes(
    String canal, TipoPersona tipo, TipoJuridico tipoJuridico, Boolean activo, UUID categoriaId) {

  public static FiltroDonantes ninguno() {
    return new FiltroDonantes(null, null, null, null, null);
  }
}
