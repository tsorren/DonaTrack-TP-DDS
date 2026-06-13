package grupo5.common.logistica.envios.commands;

import java.util.List;
import java.util.UUID;

public record RegistrarEntregaDTO(UUID donacionId, List<String> urlsFotosRecepcion) {}
