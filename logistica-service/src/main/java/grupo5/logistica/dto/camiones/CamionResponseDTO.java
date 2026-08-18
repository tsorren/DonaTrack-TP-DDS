package grupo5.logistica.dto.camiones;

import grupo5.logistica.models.entities.camiones.EstadoCamion;
import java.util.UUID;

public record CamionResponseDTO(
    UUID id,
    String patente,
    Float capacidadVolumen,
    Float altura,
    Float capacidadKG,
    EstadoCamion estado,
    UUID rutaId) {}
