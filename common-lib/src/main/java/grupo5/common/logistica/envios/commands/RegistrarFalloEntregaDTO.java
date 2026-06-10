package grupo5.common.logistica.envios.commands;

import java.util.UUID;

public record RegistrarFalloEntregaDTO(UUID donacionId, String motivoFallo) {}
