package grupo5.donaciones.graphql.limites;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * El contador de consultas rechazadas por un límite (M08, SPEC-11 G3): {@value #NOMBRE}, con la
 * etiqueta {@code motivo}. La duración de cada operación no se mide acá: la deja la observación
 * {@code graphql.request} de Spring for GraphQL.
 */
@Component
public class RechazosDeConsulta {

  /** El nombre del contador. */
  public static final String NOMBRE = "donatrack.graphql.rechazos";

  /** La etiqueta con el motivo del rechazo. */
  public static final String MOTIVO = "motivo";

  private final MeterRegistry registro;

  public RechazosDeConsulta(MeterRegistry registro) {
    this.registro = registro;
  }

  /** Suma un rechazo con su motivo. */
  public void contar(MotivoDeRechazo motivo) {
    registro.counter(NOMBRE, MOTIVO, motivo.etiqueta()).increment();
  }
}
