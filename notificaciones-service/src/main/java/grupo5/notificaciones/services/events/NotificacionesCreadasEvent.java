package grupo5.notificaciones.services.events;

import org.springframework.context.ApplicationEvent;

public class NotificacionesCreadasEvent extends ApplicationEvent {
    public NotificacionesCreadasEvent(Object source) {
        super(source);
    }
}
