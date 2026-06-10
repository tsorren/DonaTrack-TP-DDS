package grupo5.donaciones.models.ports;

import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.util.List;

public interface Segmentador {
  List<DonacionIndependiente> segmentar(Donacion donacionOriginal);
}
