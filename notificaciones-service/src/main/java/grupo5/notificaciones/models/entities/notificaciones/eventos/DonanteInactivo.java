package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.persona.Persona;

import java.time.LocalDateTime;
import java.util.List;

public class DonanteInactivo extends EventoNotificable{
    private Integer diasInactivo;

    public DonanteInactivo(Persona persona, Integer diasInactivo, LocalDateTime fecha) {
        super();
        this.diasInactivo = diasInactivo;
    }

    @Override
    public List<Notificacion> generarNotificaciones() {
        Notificacion notificacion =
                new Notificacion(this.getPersona(), "Dias de inactividad" + "\n" + diasInactivo);

        return List.of(notificacion);
    }
}
