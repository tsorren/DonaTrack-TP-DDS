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

  public DonacionIndependiente confirmar(Necesidad necesidad) {
    DonacionIndependiente donacionAAsignar;
    if (donacionOriginal.getCantidad() > cantidadNecesaria) {
      donacionAAsignar = donacionOriginal.fragmentarse(cantidadNecesaria);
    } else {
      donacionAAsignar = donacionOriginal;
    }
    donacionAAsignar.setAsignadaA(necesidad);
    donacionAAsignar.asignar();
    necesidad.asignarDonacion(donacionAAsignar);
    return donacionAAsignar;
  }
}
