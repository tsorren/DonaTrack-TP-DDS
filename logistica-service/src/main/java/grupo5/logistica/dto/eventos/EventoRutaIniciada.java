package grupo5.logistica.dto.eventos;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Evento publicado por Logística cuando un chofer inicia su ruta. */
public record EventoRutaIniciada(
    UUID rutaId,
    UUID camionId,
    String patenteCamion,
    /* IDs de las donaciones independientes que forman parte de esta ruta. */
    List<UUID> donacionesIndependientesIds,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime fechaInicio,
    String urlMapa) {}
