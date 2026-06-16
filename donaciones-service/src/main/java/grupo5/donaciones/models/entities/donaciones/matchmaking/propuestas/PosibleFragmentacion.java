package grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas;

import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PosibleFragmentacion {
  Long id;
  DonacionIndependiente donacionOriginal;
  Integer cantidadNecesaria;

  public DonacionIndependiente confirmar(Necesidad necesidad, String actor) {
    DonacionIndependiente donacionAsignar;
    if (donacionOriginal.getCantidad() > cantidadNecesaria) {
      donacionAsignar = donacionOriginal.fragmentarse(cantidadNecesaria);
    } else {
      donacionAsignar = donacionOriginal;
    }
    donacionAsignar.asignar(actor, necesidad);
    necesidad.asignarDonacion(donacionAsignar);
    return donacionAsignar;
  }
}
