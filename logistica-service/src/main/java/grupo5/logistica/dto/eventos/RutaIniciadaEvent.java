package grupo5.logistica.dto.eventos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Evento publicado por Logística cuando un chofer inicia su ruta. */
public record RutaIniciadaEvent(
    UUID rutaId,
    UUID camionId,
    String patenteCamion,
    /* IDs de las donaciones independientes que forman parte de esta ruta. */
    List<UUID> donacionesIndependientesIds,
    LocalDateTime fechaInicio) {}
