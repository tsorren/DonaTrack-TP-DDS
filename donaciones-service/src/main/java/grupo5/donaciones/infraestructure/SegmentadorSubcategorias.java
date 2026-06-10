package grupo5.donaciones.infraestructure;

import grupo5.donaciones.models.entities.categorias.SubCategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.ports.Segmentador;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class SegmentadorSubcategorias implements Segmentador {

  @Override
  public List<DonacionIndependiente> segmentar(Donacion donacionOriginal) {

    return donacionOriginal.getItems().stream().collect(toItemSubcateoriaMap()).entrySet().stream()
        .map(m -> toDonacionIndependiente(donacionOriginal, m))
        .toList();
  }

  private static DonacionIndependiente toDonacionIndependiente(
      Donacion donacionOriginal, Map.Entry<SubCategoria, List<ItemDonacion>> e) {
    return new DonacionIndependiente(
        donacionOriginal,
        e.getKey(),
        e.getValue().stream().map(ItemDonacion::toItemDonacionIndependiente).toList());
  }

  private static Collector<ItemDonacion, ?, Map<SubCategoria, List<ItemDonacion>>>
      toItemSubcateoriaMap() {
    return Collectors.groupingBy(i -> i.getBien().getSubcategoria());
  }
}

/*
List<ItemDonacion> -> Map<Subcategoria, List<ItemDonacion> -> List<DonacionIndependiente>

 */
