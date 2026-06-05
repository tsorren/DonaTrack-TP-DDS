package grupo5.donaciones.models.entities.donaciones.segmentaciones.segmentadores;

import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionSegmentada;

public interface Segmentador {
  DonacionSegmentada segmentar(Donacion donacion);
}
