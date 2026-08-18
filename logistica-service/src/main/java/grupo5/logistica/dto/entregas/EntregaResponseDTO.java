package grupo5.logistica.dto.entregas;

import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EntregaResponseDTO(
    UUID id,
    UUID idRuta,
    UUID idDonacion,
    UUID idBeneficiaria,
    DireccionDTO destino,
    EstadoEntrega estadoActual,
    LocalDateTime horaSalida,
    LocalDateTime horaArribo,
    String fotoRecepcionUrl,
    float pesoTotalKG,
    float volumenTotalM3,
    List<CambioEstadoEntregaResponseDTO> historialEstado) {}
