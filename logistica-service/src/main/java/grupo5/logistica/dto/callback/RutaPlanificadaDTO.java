package grupo5.logistica.dto.callback;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RutaPlanificadaDTO(
    @NotNull UUID camionId,
    @NotNull UUID choferId,
    @NotNull LocalDate fecha,
    @NotNull @NotEmpty List<UUID> entregaIds) {}
