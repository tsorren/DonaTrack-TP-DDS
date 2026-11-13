package grupo5.donaciones.dto.donaciones.outputs;

import java.time.OffsetDateTime;

/**
 * Contadores públicos de la landing (HB-02, RF-01). Totales desde el inicio, sin datos personales.
 *
 * @param donantesActivos donantes activos con al menos una donación registrada
 * @param donacionesRealizadas donaciones independientes entregadas y confirmadas por la entidad
 * @param beneficiariosActivos entidades activas, las mismas de {@code GET /api/entidades/publicas}
 * @param calculadoEn momento del cálculo, para que la pantalla diga "al ..."
 */
public record ContadoresPublicosDTO(
    long donantesActivos,
    long donacionesRealizadas,
    long beneficiariosActivos,
    OffsetDateTime calculadoEn) {}
