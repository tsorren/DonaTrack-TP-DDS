package grupo5.donaciones.infraestructure;

import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.ports.Segmentador;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractSegmentador<K> implements Segmentador {

  @Override
  public List<DonacionIndependiente> segmentar(List<ItemDonacionNormalizado> itemsNormalizados) {
    Map<K, List<ItemDonacionNormalizado>> grouped = agrupar(itemsNormalizados);
    return crearDonacionesIndependientes(grouped);
  }

  private Map<K, List<ItemDonacionNormalizado>> agrupar(
      List<ItemDonacionNormalizado> itemsNormalizados) {
    return itemsNormalizados.stream()
        .collect(Collectors.groupingBy(this::obtenerClaveDeAgrupacion));
  }

  protected abstract K obtenerClaveDeAgrupacion(ItemDonacionNormalizado item);

  private List<DonacionIndependiente> crearDonacionesIndependientes(
      Map<K, List<ItemDonacionNormalizado>> grouped) {
    return grouped.values().stream().map(this::crearDonacionIndependienteDesdeGrupo).toList();
  }

  private DonacionIndependiente crearDonacionIndependienteDesdeGrupo(
      List<ItemDonacionNormalizado> grupo) {
    ItemDonacionNormalizado primerItem = grupo.getFirst();
    Donacion donacionOriginal = primerItem.getDonacionOriginal();
    Subcategoria subcategoria = primerItem.getBien().getSubcategoria();

    List<ItemDonacionIndependiente> itemsIndependientes = crearItemsIndependientes(grupo);

    return new DonacionIndependiente(donacionOriginal, subcategoria, itemsIndependientes);
  }

  private List<ItemDonacionIndependiente> crearItemsIndependientes(
      List<ItemDonacionNormalizado> grupo) {
    return grupo.stream()
        .map(item -> new ItemDonacionIndependiente(item.getBien(), item.getCantidad()))
        .toList();
  }
}
