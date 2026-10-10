package grupo5.incentivos.models.entities.misiones;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.insignias.Insignia;
import java.time.LocalDate;
import java.util.UUID;

public record MisionEstado(
    UUID id,
    Integer numeroMision,
    String nombre,
    String descripcion,
    CategoriaDonante categoria,
    Integer objetivo,
    Integer progresoActual,
    boolean completada,
    LocalDate fechaCompletada,
    Insignia insignia) {}
