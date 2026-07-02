package grupo5.donaciones.dto.comunicaciones;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RutaIniciadaEvent(
    UUID rutaId,
    UUID camionId,
    String patenteCamion,
    List<UUID> donacionesIndependientesIds,
    LocalDateTime fechaInicio,
    String urlMapa) {}
