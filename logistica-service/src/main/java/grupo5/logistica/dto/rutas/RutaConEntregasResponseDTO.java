package grupo5.logistica.dto.rutas;

import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RutaConEntregasResponseDTO(
        UUID id,
        LocalDate fecha,
        List<EntregaResponseDTO> entregas,
        UUID choferId,
        UUID camionId,
        EstadoRuta estado,
        LocalDateTime horaInicioReal,
        LocalDateTime horaFinReal,
        String urlSeguimiento) {}