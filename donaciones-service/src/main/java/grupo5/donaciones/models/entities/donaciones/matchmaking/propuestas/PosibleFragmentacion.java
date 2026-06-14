package grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas;

import grupo5.donaciones.models.entities.beneficiarios.Necesidad;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionIndependiente;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PosibleFragmentacion {
    Long id;
    DonacionIndependiente donacionOriginal;
    Integer cantidadNecesaria;

    void confirmar(Necesidad necesidad){
    necesidad.seg

    }
}

