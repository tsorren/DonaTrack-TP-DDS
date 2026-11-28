package grupo5.donaciones.graphql.relaciones;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Arma el resultado de un {@code @BatchMapping} hacia un solo elemento (SPEC-11, G2): junta los ids
 * de toda la página sin nulos ni repetidos, los lee con una sola llamada al servicio y le asigna a
 * cada padre el elemento de su id. Un padre sin id, o con un id que el servicio no devolvió, no
 * entra en el mapa y su campo queda nulo. Sin ids no llama al servicio.
 */
final class CargaPorId {

  private CargaPorId() {}

  static <P, D, V> Map<P, V> unir(
      List<P> padres,
      Function<P, UUID> idDelPadre,
      Function<Collection<UUID>, List<D>> leer,
      Function<D, UUID> idDelLeido,
      Function<D, V> mapear) {
    Set<UUID> ids = new LinkedHashSet<>();
    padres.stream().map(idDelPadre).filter(Objects::nonNull).forEach(ids::add);
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<UUID, V> porId = new HashMap<>();
    for (D leido : leer.apply(ids)) {
      porId.putIfAbsent(idDelLeido.apply(leido), mapear.apply(leido));
    }
    Map<P, V> resultado = new HashMap<>();
    for (P padre : padres) {
      V valor = porId.get(idDelPadre.apply(padre));
      if (valor != null) {
        resultado.put(padre, valor);
      }
    }
    return resultado;
  }
}
