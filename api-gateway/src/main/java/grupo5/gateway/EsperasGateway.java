package grupo5.gateway;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.server.PathContainer;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Límites de espera del gateway hacia los servicios (R11-04): ninguna llamada espera sin límite.
 * Pasado el de conexión o el de lectura, el gateway contesta 504 y el cliente liviano lo lleva a su
 * página de falla con el código de soporte. Los del cliente liviano son algo mayores, para que el
 * gateway conteste primero.
 *
 * <p>La subida de un archivo (el CSV de donantes y las fotos del acta) tiene su propio límite, más
 * largo. La fábrica la reconoce por método y ruta, que el gateway no reescribe (R1 del ADR-E6-02).
 */
@Configuration(proxyBeanMethods = false)
public class EsperasGateway {

  /** Valores en {@code donatrack.gateway.limites.*}. */
  @ConfigurationProperties("donatrack.gateway.limites")
  public record Limites(
      @DefaultValue("3s") Duration conexion,
      @DefaultValue("10s") Duration lectura,
      @DefaultValue("100s") Duration subida) {}

  private static final List<PathPattern> RUTAS_DE_SUBIDA =
      List.of("/api/donantes/archivos", "/api/entregas/{id}/fotos").stream()
          .map(PathPatternParser.defaultInstance::parse)
          .toList();

  static boolean esSubida(URI uri, HttpMethod metodo) {
    PathContainer ruta = PathContainer.parsePath(uri.getRawPath());
    return HttpMethod.POST.equals(metodo)
        && RUTAS_DE_SUBIDA.stream().anyMatch(patron -> patron.matches(ruta));
  }

  /** El gateway arma su cliente HTTP con esta fábrica (la toma el cliente de la ruta). */
  @Bean
  ClientHttpRequestFactory fabricaConLimites(Limites limites) {
    HttpClient http = HttpClient.newBuilder().connectTimeout(limites.conexion()).build();
    JdkClientHttpRequestFactory comun = new JdkClientHttpRequestFactory(http);
    comun.setReadTimeout(limites.lectura());
    JdkClientHttpRequestFactory deSubida = new JdkClientHttpRequestFactory(http);
    deSubida.setReadTimeout(limites.subida());
    return new ClientHttpRequestFactory() {
      @Override
      public ClientHttpRequest createRequest(URI uri, HttpMethod metodo) throws IOException {
        return (esSubida(uri, metodo) ? deSubida : comun).createRequest(uri, metodo);
      }
    };
  }

  /** Un servicio que no conecta o no responde a tiempo es 504, no un error nuestro sin explicar. */
  @RestControllerAdvice
  static class ServicioSinRespuesta {

    private static final Logger log = LoggerFactory.getLogger(ServicioSinRespuesta.class);

    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<Void> sinRespuesta(ResourceAccessException error) {
      log.warn("[GATEWAY] El servicio no respondió a tiempo: {}", error.getMessage());
      return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
    }
  }
}
