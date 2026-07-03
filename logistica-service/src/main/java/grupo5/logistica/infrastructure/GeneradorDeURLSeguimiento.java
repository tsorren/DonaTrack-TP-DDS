package grupo5.logistica.infrastructure;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeneradorDeURLSeguimiento {

  private final String baseUrl;

  public GeneradorDeURLSeguimiento(@Value("${logistica.tracking.base-url}") String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  /**
   * Genera la URL de seguimiento para una ruta puntual.
   *
   * @param rutaId identificador de la ruta ya iniciada que se quiere seguir en el mapa.
   * @return URL completa que el front de seguimiento puede resolver para mostrar la posición del
   *     camión en tiempo real.
   */
  public String generarUrl(UUID rutaId) {
    if (rutaId == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    return baseUrl + "/" + rutaId;
  }
}
