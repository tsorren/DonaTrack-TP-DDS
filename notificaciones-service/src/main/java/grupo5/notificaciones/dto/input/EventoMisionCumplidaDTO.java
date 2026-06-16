package grupo5.notificaciones.dto.input;

import grupo5.notificaciones.models.entities.persona.Persona;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EventoMisionCumplidaDTO {
  private Persona persona;
  private LocalDateTime fecha = LocalDateTime.now();
  private String nombreMision;
  private String recompensa;
}
