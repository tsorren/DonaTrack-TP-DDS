package grupo5.notificaciones.infrastructure.mockEnvios;

import grupo5.notificaciones.infrastructure.CorreoAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CorreoEnvioMock implements CorreoAdapter {
  private static final Logger log = LoggerFactory.getLogger(CorreoEnvioMock.class);

  @Override
  public boolean enviarMail(String destinatario, String mensaje) {

    log.info(
        """
            [EMAIL SIMULADO]

            Destinatario: {}
            Mensaje: {}
            """,
        destinatario,
        mensaje);

    return true;
  }
}
