package grupo5.donaciones.models.entities.donaciones.matchmaking.algoritmos;

import grupo5.donaciones.models.entities.beneficiarios.Necesidad;
import java.util.List;

import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionIndependiente;
import lombok.Getter;
import lombok.Setter;


public class AlgoritmoCompatibilidadSemantica {
    Integer calcularScoreSemantico(Necesidad necesidad, DonacionIndependiente donacion){

    }

    Integer contarPalabrasEnComun(Necesidad necesidad, DonacionIndependiente donacion){
        return comparadorString(necesidad.getDescripcion(), donacion. descripcionDonaciones)
    }

    List<Necesidad> ordenarNecesidades(List<Necesidad>){

    }

    List<DonacionIndependiente> filtrarDonaciones(Necesidad, List<DonacionIndependiente>){

    }

    Propuesta armarPropuestaPara(Necesidad, List<DonacionIndependiente>){

    }

}
