package grupo5.logistica.dto.entregas;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representación pública y sanitizada de una entrega completada para visualización en el mapa
 * público (RF-02). No incluye dirección exacta (calle, altura, piso, departamento, código postal),
 * identificadores de terceros (beneficiario, donación, ruta), historial de actores ni fotos de
 * recepción.
 */
public record EntregaMapaPublicoResponseDTO(
    UUID idEntrega,
    double latitud,
    double longitud,
    String localidad,
    String provincia,
    String pais,
    LocalDateTime fechaEntrega,
    float pesoTotalKG,
    float volumenTotalM3) {}
