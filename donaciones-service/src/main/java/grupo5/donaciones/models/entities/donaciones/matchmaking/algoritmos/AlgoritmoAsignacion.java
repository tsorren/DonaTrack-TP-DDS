package grupo5.donaciones.models.entities.donaciones.matchmaking.algoritmos;

import grupo5.donaciones.models.entities.beneficiarios.Necesidad;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionIndependiente;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public abstract class AlgoritmoAsignacion{
    List<Propuesta> ejecutar(List<Necesidad> necesidades, List<DonacionIndependiente> donaciones){

        Propuesta propuesta;
        return propuesta;
    }
    List<Necesidad> ordenarNecesidades(List<Necesidad> necesidades){

    }
    List<DonacionIndependiente> filtrarDonaciones(Necesidad necesidad, List<DonacionIndependiente> donaciones){

    }
    Propuesta armarPropuestaPara(Necesidad necesidades, List<DonacionIndependiente> donaciones){

    }
}
