package grupo5.incentivos.dto;

import java.util.UUID;

public record RegistrarDonanteRequest(UUID idDonante, UUID idPersona, String nombre) {}
