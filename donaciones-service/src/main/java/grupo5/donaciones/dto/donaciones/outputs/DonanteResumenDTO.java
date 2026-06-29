package grupo5.donaciones.dto.donaciones.outputs;

import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import java.util.UUID;

public record DonanteResumenDTO(UUID idDonante, UUID personaId, PersonaOutputDTO persona) {}
