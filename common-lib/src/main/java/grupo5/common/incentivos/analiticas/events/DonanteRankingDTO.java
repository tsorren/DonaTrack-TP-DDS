package grupo5.common.incentivos.analiticas.events;

import java.util.UUID;

public record DonanteRankingDTO(UUID donanteId, String nombre, int misionesCompletadas) {}
