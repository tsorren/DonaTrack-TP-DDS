package grupo5.gateway;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.local.LocalBucketBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Almacén de cubetas para los tests que no necesitan Redis: un mapa en memoria. */
class CubetasEnMemoria implements LimiteDeSolicitudes.Almacen {

  private final Map<String, Bucket> cubetas = new ConcurrentHashMap<>();

  @Override
  public Bucket cubeta(String clave, Supplier<BucketConfiguration> configuracion) {
    return cubetas.computeIfAbsent(clave, k -> nueva(configuracion.get()));
  }

  int cantidad() {
    return cubetas.size();
  }

  private static Bucket nueva(BucketConfiguration configuracion) {
    LocalBucketBuilder builder = Bucket.builder();
    for (var limite : configuracion.getBandwidths()) {
      builder.addLimit(limite);
    }
    return builder.build();
  }
}
