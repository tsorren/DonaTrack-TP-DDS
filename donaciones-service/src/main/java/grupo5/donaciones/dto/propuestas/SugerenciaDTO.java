package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.models.entities.propuestas.TipoAlgoritmo;

/**
 * Qué algoritmo sugirió la propuesta, en qué puesto de su lista de esa corrida (1 es la primera) y
 * de cuántas; {@code eligioLasDonaciones}: las donaciones de la propuesta son las suyas (R11-24).
 */
public record SugerenciaDTO(
    TipoAlgoritmo algoritmo, int puesto, int deCuantas, boolean eligioLasDonaciones) {}
