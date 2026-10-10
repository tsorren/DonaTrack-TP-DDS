package grupo5.logistica.dto.rutas;

import grupo5.logistica.models.entities.rutas.EstadoRuta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CambioEstadoRutaRequestDTO(
    @NotNull EstadoRuta estado, UUID choferId, @NotBlank String actor) {}
