package grupo5.donaciones.dto.comunicaciones;

import java.util.UUID;

public record RegistrarDonanteRequest(UUID idDonante, UUID idPersona, String nombre) {}
