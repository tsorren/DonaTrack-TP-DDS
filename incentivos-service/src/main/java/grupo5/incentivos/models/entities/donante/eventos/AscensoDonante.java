package grupo5.incentivos.models.entities.donante.eventos;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import java.util.UUID;

public record AscensoDonante(
    UUID IdDonante,
    UUID idPersona,
    CategoriaDonante categoriaAnterior,
    CategoriaDonante categoriaNueva) {}
