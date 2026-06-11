package grupo5.common.logistica.envios.commands;

import java.util.List;
import java.util.UUID;

public record PlanificarRutaDTO(List<UUID> donacionIds, UUID camionId, List<String> puntosRuta) {}
