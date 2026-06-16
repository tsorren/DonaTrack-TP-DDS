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
        String mensaje = String.format(
                "Hace %d días que no registramos una donación tuya. " +
                "Tu colaboración es muy importante y podría ayudar a quienes más lo necesitan. " +
                "Si estás en condiciones de donar nuevamente, te invitamos a colaborar!.",
                diasInactivo
        );
        Notificacion notificacion =
                new Notificacion(this.getPersona(), mensaje);

        return List.of(notificacion);
    }
}
