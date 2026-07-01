package grupo5.logistica.dto.eventos;

import grupo5.logistica.models.entities.eventos.TipoEventoLogistico;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventoLogisticoResponseDTO(
    UUID id,
    TipoEventoLogistico tipo,
    UUID rutaId,
    UUID entregaId,
    UUID idDonacion,
    UUID idBeneficiaria,
    LocalDateTime fechaCreacion,
    boolean procesado,
    LocalDateTime fechaProcesado) {}
