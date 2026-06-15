package grupo5.notificaciones.services;
import grupo5.notificaciones.models.repositories.NotificacionRepository;

public class NotificacionService {
    private final NotificacionRepository repository;

    public NotificacionService(NotificacionRepository repository) {
        this.repository = repository;
    }
}
