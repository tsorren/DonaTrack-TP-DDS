package grupo5.notificaciones.dto.input;

import grupo5.notificaciones.models.entities.persona.Persona;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventoSubioCategoriaDTO {
    private Persona persona;
    private LocalDateTime fecha = LocalDateTime.now();
    private String categoriaNueva;
    private String categoriaVieja;
}
