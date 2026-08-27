package grupo5.logistica.dto.rutas;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AgregarEntregaRutaRequestDTO(@NotNull UUID entregaId) {}
