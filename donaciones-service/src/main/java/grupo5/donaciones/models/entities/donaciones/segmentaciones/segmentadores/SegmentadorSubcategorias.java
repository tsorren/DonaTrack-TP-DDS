package grupo5.donaciones.models.entities.donaciones.segmentaciones.segmentadores;

import grupo5.donaciones.models.entities.bienes.SubCategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionIndependiente;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionSegmentada;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class SegmentadorSubcategorias implements Segmentador {

  @Override
  public DonacionSegmentada segmentar(Donacion donacion) {

    List<DonacionIndependiente> donacionesIndependientes =
        donacion.getItems().stream().collect(toItemSubcateoriaMap()).entrySet().stream()
            .map(SegmentadorSubcategorias::toDonacionIndependiente)
            .toList();

    return new DonacionSegmentada(donacion, donacionesIndependientes);
  }

  private static DonacionIndependiente toDonacionIndependiente(
      Map.Entry<SubCategoria, List<ItemDonacion>> e) {
    return new DonacionIndependiente(
        e.getKey(), e.getValue().stream().map(ItemDonacion::toItemDonacionIndependiente).toList());
  }

  private static Collector<ItemDonacion, ?, Map<SubCategoria, List<ItemDonacion>>>
      toItemSubcateoriaMap() {
    return Collectors.groupingBy(i -> i.getBien().getSubcategoria());
  }
}

/*
List<ItemDonacion> -> Map<Subcategoria, List<ItemDonacion> -> List<DonacionIndependiente>

 */
