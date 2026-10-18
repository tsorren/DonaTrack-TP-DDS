package grupo5.donaciones.dto.donacionesIndependientes;

import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CambioEstadoDonacionIndependienteRequestDTO(
    @NotNull(message = "El estado es obligatorio") TipoEstadoDonacion estado,
    String justificacion,
    UUID necesidadId,
    String urlMapa,
    String patenteCamion,
    Boolean replanificable) {}
