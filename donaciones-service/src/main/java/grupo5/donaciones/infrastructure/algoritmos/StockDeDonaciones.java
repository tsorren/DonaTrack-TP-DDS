package grupo5.donaciones.infrastructure.algoritmos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import java.util.*;

public class StockDeDonaciones {

  private final Map<DonacionIndependiente, Integer> cantidades = new HashMap<>();

  public StockDeDonaciones(List<DonacionIndependiente> donaciones) {
    if (donaciones == null) throw new ValidationException(ErrorCatalog.STOCK_LISTA_DONACIONES_NULA);
    for (DonacionIndependiente donacion : donaciones) {
      cantidades.put(donacion, donacion.getCantidad());
    }
  }

  public List<DonacionIndependiente> disponibles() {
    List<DonacionIndependiente> resultado = new ArrayList<>();
    for (Map.Entry<DonacionIndependiente, Integer> entrada : cantidades.entrySet()) {
      if (entrada.getValue() > 0) {
        resultado.add(entrada.getKey());
      }
    }
    return resultado;
  }

  public int disponibleDe(DonacionIndependiente donacion) {
    return cantidades.getOrDefault(donacion, 0);
  }

  public void registrarReservas(Propuesta propuesta) {
    for (PosibleFragmentacion fragmentacion : propuesta.getPosiblesFragmentaciones()) {
      UUID donacionId = fragmentacion.getDonacionOriginalId();
      if (donacionId == null) continue;
      cantidades.entrySet().stream()
          .filter(e -> e.getKey().getId().equals(donacionId))
          .findFirst()
          .ifPresent(
              entry -> {
                int restante = entry.getValue() - fragmentacion.getCantidadNecesaria();
                cantidades.put(entry.getKey(), restante);
              });
    }
  }
}
