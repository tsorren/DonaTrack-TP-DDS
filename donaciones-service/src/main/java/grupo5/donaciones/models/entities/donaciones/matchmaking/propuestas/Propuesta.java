package grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas;

import grupo5.donaciones.models.entities.beneficiarios.Necesidad;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class Propuesta {
    Long id;
    Necesidad necesidadQueSatisface;
    List<PosibleFragmentacion> posiblesFragmentaciones;
    EstadoPropuesta estado;
    LocalDateTime fechaCreacion;



}