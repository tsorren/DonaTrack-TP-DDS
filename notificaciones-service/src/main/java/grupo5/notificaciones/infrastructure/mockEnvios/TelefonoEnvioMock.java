package grupo5.notificaciones.infrastructure.mockEnvios;

import grupo5.notificaciones.infrastructure.TelefonoAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelefonoEnvioMock implements TelefonoAdapter {
  private static final Logger log = LoggerFactory.getLogger(TelefonoEnvioMock.class);

  @Override
  public boolean enviarSms(String telefono, String mensaje) {

    log.info(
        """
            [SMS SIMULADO]

            Telefono: {}
            Mensaje: {}
            """,
        telefono,
        mensaje);

    return true;
  }
}
