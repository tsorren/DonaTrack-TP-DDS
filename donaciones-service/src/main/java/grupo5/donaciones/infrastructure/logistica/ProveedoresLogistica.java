package grupo5.donaciones.infrastructure.logistica;

import grupo5.donaciones.services.logistica.ICatalogoProveedoresLogistica;
import grupo5.donaciones.services.logistica.IProveedorLogistica;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Adapters de proveedores de logística armados a partir de la configuración, indexados por id. */
public class ProveedoresLogistica implements ICatalogoProveedoresLogistica {

  private final Map<String, IProveedorLogistica> porId;

  public ProveedoresLogistica(List<IProveedorLogistica> proveedores) {
    this.porId =
        proveedores.stream()
            .collect(Collectors.toUnmodifiableMap(IProveedorLogistica::id, Function.identity()));
  }

  public Optional<IProveedorLogistica> buscar(String proveedorId) {
    return Optional.ofNullable(porId.get(proveedorId));
  }

  @Override
  public boolean tieneAdapter(String proveedorId) {
    return porId.containsKey(proveedorId);
  }

  public int cantidad() {
    return porId.size();
  }
}
