package grupo5.notificaciones.services;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EventoNotificable;
import grupo5.notificaciones.models.repositories.NotificacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificacionService {
    private final NotificacionRepository repository;

    public NotificacionService(NotificacionRepository repository) {
        this.repository = repository;
    }

    public void procesar(EventoNotificable evento) {
        List<Notificacion> notificaciones = evento.generarNotificaciones();

        for (Notificacion notificacion : notificaciones) {
            notificacion.notificar(null);
            repository.save(notificacion);
        }
    }
}
