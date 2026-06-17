package grupo5.notificaciones.dto.input;

import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EventoDonacionDTO {
  private Persona persona;
  private LocalDateTime fecha;
  private Persona entidadBeneficiaria;
  private String detalleDonacion;
}
