package grupo5.logistica.dto.rutas;

import grupo5.logistica.models.entities.rutas.EstadoRuta;
import java.util.UUID;

public record CambioEstadoRutaRequestDTO(EstadoRuta estado, UUID choferId, String actor) {}
