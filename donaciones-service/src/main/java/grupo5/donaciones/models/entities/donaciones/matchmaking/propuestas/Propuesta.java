package grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas;


import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Propuesta {
    Long id;
    Necesidad necesidadQueSatisface;
    List<PosibleFragmentacion> posiblesFragmentaciones;
    EstadoPropuesta estado;
    LocalDateTime fechaCreacion;

    public void agregarFragmentacion(DonacionIndependiente donacion, int cantidad) {
        if (posiblesFragmentaciones == null) posiblesFragmentaciones = new ArrayList<>();
        PosibleFragmentacion f = new PosibleFragmentacion();
        f.setDonacionOriginal(donacion);
        f.setCantidadNecesaria(cantidad);
        posiblesFragmentaciones.add(f);
    }

    public boolean estaActiva() {
        return this.estado != null && this.estado != EstadoPropuesta.DESCARTADA;
    }

    public void confirmar(){
        posiblesFragmentaciones.forEach(f -> f.confirmar(necesidadQueSatisface));
        this.estado = EstadoPropuesta.APROBADA;
    }

    public void rechazar(){
        this.estado = EstadoPropuesta.DESCARTADA;
    }
}