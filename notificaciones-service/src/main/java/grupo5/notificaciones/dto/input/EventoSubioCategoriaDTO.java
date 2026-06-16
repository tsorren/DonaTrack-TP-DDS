package grupo5.notificaciones.dto.input;

import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EventoSubioCategoriaDTO {
  private Persona persona;
  private LocalDateTime fecha = LocalDateTime.now();
  private String categoriaNueva;
  private String categoriaVieja;
}
