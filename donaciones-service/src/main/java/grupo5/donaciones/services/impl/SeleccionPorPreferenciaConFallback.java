package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.services.logistica.IEstrategiaSeleccionProveedor;
import grupo5.donaciones.services.logistica.IPreferenciaProveedor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Estrategia base: primero el proveedor preferido y, como reenvío, el resto en el orden de la
 * configuración. El preferido arranca con el valor configurado y se puede cambiar en caliente (no
 * persiste: al reiniciar vuelve al configurado).
 */
@Component
public class SeleccionPorPreferenciaConFallback
    implements IEstrategiaSeleccionProveedor, IPreferenciaProveedor {

  private final List<String> configurados;
  private final AtomicReference<List<String>> orden;

  public SeleccionPorPreferenciaConFallback(
      @Value("${donatrack.logistica.proveedores:donatrack}") List<String> proveedores,
      @Value("${donatrack.logistica.proveedor-preferido:donatrack}") String preferido) {
    List<String> normalizados =
        proveedores.stream().map(String::trim).filter(id -> !id.isEmpty()).distinct().toList();
    if (normalizados.isEmpty()) {
      throw new IllegalArgumentException(
          "donatrack.logistica.proveedores debe listar al menos un proveedor");
    }
    String preferidoNormalizado = preferido == null ? "" : preferido.trim();
    if (!normalizados.contains(preferidoNormalizado)) {
      throw new IllegalArgumentException(
          "El proveedor preferido '"
              + preferidoNormalizado
              + "' no está en donatrack.logistica.proveedores "
              + normalizados);
    }

    this.configurados = List.copyOf(normalizados);
    this.orden = new AtomicReference<>(ordenConPreferido(preferidoNormalizado));
  }

  private List<String> ordenConPreferido(String preferido) {
    List<String> ordenados = new ArrayList<>();
    ordenados.add(preferido);
    configurados.stream().filter(id -> !id.equals(preferido)).forEach(ordenados::add);
    return List.copyOf(ordenados);
  }

  @Override
  public List<String> ordenar(DatosEntregaLogistica datos) {
    return orden.get();
  }

  @Override
  public List<String> proveedoresConfigurados() {
    return configurados;
  }

  @Override
  public String proveedorPreferido() {
    return orden.get().get(0);
  }

  @Override
  public void cambiarProveedorPreferido(String proveedorId) {
    String normalizado = proveedorId == null ? "" : proveedorId.trim();
    if (!configurados.contains(normalizado)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    orden.set(ordenConPreferido(normalizado));
  }
}
