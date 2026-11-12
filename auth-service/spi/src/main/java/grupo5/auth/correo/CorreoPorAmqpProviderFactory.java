package grupo5.auth.correo;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.EmailTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Registra {@link CorreoPorAmqpProvider} en Keycloak con el id {@code donatrack-amqp}. Para usarlo
 * hay que elegirlo como proveedor de plantillas de correo ({@code spi-email-template--provider}) y
 * darle la conexión a RabbitMQ ({@code spi-email-template--donatrack-amqp--host}, {@code --user},
 * {@code --password}, etc.).
 */
public class CorreoPorAmqpProviderFactory implements EmailTemplateProviderFactory {

  public static final String ID = "donatrack-amqp";
  private static final Logger LOG = Logger.getLogger(CorreoPorAmqpProviderFactory.class);

  private PublicadorDeEventos publicador;

  @Override
  public EmailTemplateProvider create(KeycloakSession session) {
    return new CorreoPorAmqpProvider(session, publicador);
  }

  @Override
  public void init(Config.Scope config) {
    ConfiguracionAmqp amqp = ConfiguracionAmqp.desde(config);
    this.publicador = new PublicadorRabbitMq(amqp);
    if (!amqp.completa()) {
      // No se corta el arranque: el proveedor se registra siempre, aunque no sea el elegido.
      LOG.warn("El proveedor de correo por AMQP no tiene clave de RabbitMQ: no podrá publicar");
    }
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    // Sin trabajo posterior a la inicialización.
  }

  @Override
  public void close() {
    // No guarda recursos: cada publicación abre y cierra su conexión.
  }

  @Override
  public String getId() {
    return ID;
  }
}
