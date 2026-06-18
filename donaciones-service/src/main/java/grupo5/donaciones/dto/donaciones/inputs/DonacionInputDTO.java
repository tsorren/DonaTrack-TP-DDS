package grupo5.donaciones.dto.donaciones.inputs;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import java.util.List;
import java.util.UUID;

public record DonacionInputDTO(
    UUID idDonante,
    String descripcion,
    List<ItemDonacionInputDTO> items,
    String nombreDeposito,
    DireccionInputDTO direccion) {}
