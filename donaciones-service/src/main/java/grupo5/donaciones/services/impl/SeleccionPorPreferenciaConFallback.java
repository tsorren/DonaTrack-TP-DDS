package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.services.logistica.IEstrategiaSeleccionProveedor;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Estrategia base: primero el proveedor preferido y, como reenvío, el resto en el orden de la
 * configuración.
 */
@Component
public class SeleccionPorPreferenciaConFallback implements IEstrategiaSeleccionProveedor {

  private final List<String> orden;

  public SeleccionPorPreferenciaConFallback(
      @Value("${donatrack.logistica.proveedores:donatrack}") List<String> proveedores,
      @Value("${donatrack.logistica.proveedor-preferido:donatrack}") String preferido) {
    List<String> configurados =
        proveedores.stream().map(String::trim).filter(id -> !id.isEmpty()).distinct().toList();
    if (configurados.isEmpty()) {
      throw new IllegalArgumentException(
          "donatrack.logistica.proveedores debe listar al menos un proveedor");
    }
    String preferidoNormalizado = preferido == null ? "" : preferido.trim();
    if (!configurados.contains(preferidoNormalizado)) {
      throw new IllegalArgumentException(
          "El proveedor preferido '"
              + preferidoNormalizado
              + "' no está en donatrack.logistica.proveedores "
              + configurados);
    }

    List<String> ordenados = new ArrayList<>();
    ordenados.add(preferidoNormalizado);
    configurados.stream().filter(id -> !id.equals(preferidoNormalizado)).forEach(ordenados::add);
    this.orden = List.copyOf(ordenados);
  }

  @Override
  public List<String> ordenar(DatosEntregaLogistica datos) {
    return orden;
  }
}
