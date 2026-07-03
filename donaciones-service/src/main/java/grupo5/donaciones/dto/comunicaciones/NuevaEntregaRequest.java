package grupo5.donaciones.dto.comunicaciones;

import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import java.util.UUID;

public record NuevaEntregaRequest(
    UUID donacionIndependienteId,
    UUID entidadBeneficiariaId,
    DireccionOutputDTO direccionOrigen,
    DireccionOutputDTO direccionDestino,
    String descripcion) {}
