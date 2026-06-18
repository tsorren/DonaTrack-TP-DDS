package grupo5.donaciones.dto.comunicaciones;

import java.util.UUID;

public record DonacionExitosaRequest(UUID donanteId, UUID organizacionId) {}
