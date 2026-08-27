package grupo5.donaciones.dto.donaciones.inputs;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DonacionInputDTO(
    @NotNull(message = "El id del donante es obligatorio") UUID idDonante,
    String descripcion,
    @NotEmpty(message = "La donación debe contener al menos un ítem")
        List<@Valid ItemDonacionInputDTO> items,
    String nombreDeposito,
    @Valid DireccionInputDTO direccion,
    LocalDateTime fecha) {}
