package grupo5.donaciones.config;

import grupo5.donaciones.infrastructure.logistica.ProveedorLogisticaAmqp;
import grupo5.donaciones.infrastructure.logistica.ProveedoresLogistica;
import grupo5.donaciones.services.logistica.IProveedorLogistica;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Arma un adapter por cada proveedor de {@code donatrack.logistica.proveedores} según su {@code
 * donatrack.logistica.proveedor.<id>.transporte}. Un proveedor sin transporte soportado queda sin
 * adapter: el relay lo trata como rechazo y se prueba con el siguiente.
 */
@Configuration
public class LogisticaProveedoresConfig {

  private static final Logger log = LoggerFactory.getLogger(LogisticaProveedoresConfig.class);

  static final String TRANSPORTE_AMQP = "amqp";

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
