package grupo5.donaciones.dto.personas;

import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.List;
import java.util.UUID;

/**
 * Filtros opcionales de {@code GET /api/personas} (R11-01); un campo nulo no filtra. {@code id},
 * repetible y hasta 100, es una lectura por lote: solo esas personas (un id que no existe no
 * aparece).
 */
public record FiltroPersonas(TipoPersona tipo, TipoJuridico tipoJuridico, List<UUID> id) {

  public static final int IDS_MAXIMOS = 100;

  public static FiltroPersonas ninguno() {
    return new FiltroPersonas(null, null, null);
  }
}
