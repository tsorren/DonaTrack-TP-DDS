package grupo5.donaciones.config;

import grupo5.donaciones.infrastructure.logistica.ProveedorLogisticaAmqp;
import grupo5.donaciones.infrastructure.logistica.ProveedorLogisticaHttp;
import grupo5.donaciones.infrastructure.logistica.ProveedoresLogistica;
import grupo5.donaciones.services.logistica.IProveedorLogistica;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Arma un adapter por cada proveedor de {@code donatrack.logistica.proveedores} según su {@code
 * donatrack.logistica.proveedor.<id>.transporte} (amqp o http). Un proveedor sin transporte
 * soportado queda sin adapter: el relay lo trata como rechazo y se prueba con el siguiente.
 */
@Configuration
public class LogisticaProveedoresConfig {

  private static final Logger log = LoggerFactory.getLogger(LogisticaProveedoresConfig.class);

  static final String TRANSPORTE_AMQP = "amqp";
  static final String TRANSPORTE_HTTP = "http";
  static final long CONNECT_TIMEOUT_POR_DEFECTO_MS = 1000;
  static final long READ_TIMEOUT_POR_DEFECTO_MS = 3000;

  @Bean
  public ProveedoresLogistica proveedoresLogistica(
      @Value("${donatrack.logistica.proveedores:donatrack}") List<String> proveedores,
      @Value("${donatrack.logistica.acuse-timeout-ms:5000}") long acuseTimeoutMs,
      Environment environment,
      ConnectionFactory connectionFactory,
      JacksonJsonMessageConverter messageConverter) {
    RabbitTemplate templateComandos = null;
    List<IProveedorLogistica> adapters = new ArrayList<>();

    for (String id : proveedores.stream().map(String::trim).filter(p -> !p.isEmpty()).toList()) {
      String transporte =
          environment.getProperty("donatrack.logistica.proveedor." + id + ".transporte", "").trim();
      if (TRANSPORTE_AMQP.equalsIgnoreCase(transporte)) {
        if (templateComandos == null) {
          templateComandos = templateComandos(connectionFactory, messageConverter);
        }
        adapters.add(new ProveedorLogisticaAmqp(id, templateComandos, acuseTimeoutMs));
        log.info("[BROKER-LOGISTICA] Proveedor {} registrado con transporte AMQP", id);
      } else if (TRANSPORTE_HTTP.equalsIgnoreCase(transporte)) {
        armarAdapterHttp(id, environment).ifPresent(adapters::add);
      } else {
        log.warn(
            "[BROKER-LOGISTICA] Proveedor {} sin transporte soportado ('{}'): no tiene adapter y"
                + " sus envíos se tratarán como rechazados",
            id,
            transporte);
      }
    }
    return new ProveedoresLogistica(adapters);
  }

  /**
   * Arma el adapter HTTP de un proveedor con su URL y sus timeouts. Sin URL no hay adapter: el
   * relay lo trata como rechazo y se prueba con el siguiente.
   */
  private static Optional<IProveedorLogistica> armarAdapterHttp(
      String id, Environment environment) {
    String prefijo = "donatrack.logistica.proveedor." + id + ".";
    String url = environment.getProperty(prefijo + "url", "").trim();
    if (url.isEmpty()) {
      log.warn(
          "[BROKER-LOGISTICA] Proveedor {} con transporte HTTP pero sin {}url: no tiene adapter y"
              + " sus envíos se tratarán como rechazados",
          id,
          prefijo);
      return Optional.empty();
    }
    long connectTimeoutMs =
        environment.getProperty(
            prefijo + "connect-timeout-ms", Long.class, CONNECT_TIMEOUT_POR_DEFECTO_MS);
    long readTimeoutMs =
        environment.getProperty(
            prefijo + "read-timeout-ms", Long.class, READ_TIMEOUT_POR_DEFECTO_MS);
    log.info("[BROKER-LOGISTICA] Proveedor {} registrado con transporte HTTP ({})", id, url);
    return Optional.of(
        new ProveedorLogisticaHttp(id, restClient(url, connectTimeoutMs, readTimeoutMs)));
  }

  /**
   * Cliente HTTP de un proveedor. El timeout de conexión y el de lectura se separan a propósito: si
   * no se pudo conectar el pedido no salió (rechazado); si se agotó la lectura pudo haber llegado
   * (incierto).
   */
  public static RestClient restClient(String url, long connectTimeoutMs, long readTimeoutMs) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofMillis(connectTimeoutMs))
            .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
    return RestClient.builder().baseUrl(url).requestFactory(requestFactory).build();
  }

  /**
   * Template exclusivo de los comandos a logística: {@code mandatory=true} para que RabbitMQ
   * devuelva lo que no puede rutear. El template compartido de los demás eventos no cambia.
   */
  public static RabbitTemplate templateComandos(
      ConnectionFactory connectionFactory, JacksonJsonMessageConverter messageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter);
    template.setMandatory(true);
    return template;
  }
}
