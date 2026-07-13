package grupo5.logistica.dto.choferes;

import grupo5.logistica.models.entities.choferes.EstadoChofer;
import java.util.UUID;

public record ChoferResponseDTO(
    UUID id,
    String nombre,
    String apellido,
    String licencia,
    String telefonoContacto,
    EstadoChofer estado,
    UUID rutaId) {}
