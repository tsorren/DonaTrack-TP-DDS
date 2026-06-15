package grupo5.donaciones.models.ports;

import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import java.util.List;

public interface Segmentador {
  List<DonacionIndependiente> segmentar(List<ItemDonacionNormalizado> itemsNormalizados);
}
