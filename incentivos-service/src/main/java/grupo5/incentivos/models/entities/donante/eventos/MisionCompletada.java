package grupo5.incentivos.models.entities.donante.eventos;

import grupo5.incentivos.models.entities.insignias.Insignia;
import java.util.UUID;

public record MisionCompletada(
    UUID donanteId, UUID idPersona, String nombreDonante, String nombreMision, Insignia insignia) {}
