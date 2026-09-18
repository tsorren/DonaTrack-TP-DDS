package grupo5.donaciones.models.entities.itemsNormalizados;

import java.util.List;

public class EvaluadorNormalizacion {

  private EvaluadorNormalizacion() {}

  public static boolean estanTodosNormalizados(List<ItemDonacionNormalizado> items) {
    if (items == null || items.isEmpty()) {
      return false;
    }
    return items.stream().allMatch(ItemDonacionNormalizado::estaResuelto);
  }
}
