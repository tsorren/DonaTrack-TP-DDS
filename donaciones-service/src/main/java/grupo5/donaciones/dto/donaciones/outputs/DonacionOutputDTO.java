package grupo5.donaciones.dto.donaciones.outputs;

import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DonacionOutputDTO(
    UUID id,
    DonanteResumenDTO donante,
    List<ItemDonacionOutputDTO> items,
    String descripcion,
    LocalDateTime fecha,
    DireccionOutputDTO deposito,
    EstadoDonacion estadoActual,
    List<CambioEstadoOutputDTO> historialEstados) {}
