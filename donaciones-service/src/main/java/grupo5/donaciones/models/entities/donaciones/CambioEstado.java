package grupo5.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class CambioEstado {

    private final EstadoDonacion estadoAnterior;
    private final EstadoDonacion estadoNuevo;
    private final LocalDateTime timestamp;
    private final String justificacion;

    public CambioEstado(EstadoDonacion estadoAnterior, EstadoDonacion estadoNuevo, String justificacion) {
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.timestamp = LocalDateTime.now();
        this.justificacion = justificacion;
    }
}