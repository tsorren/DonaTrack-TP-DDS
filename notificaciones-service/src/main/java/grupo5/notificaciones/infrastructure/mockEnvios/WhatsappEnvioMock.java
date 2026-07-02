package grupo5.notificaciones.infrastructure.mockEnvios;

import grupo5.notificaciones.infrastructure.WhatsAppAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WhatsappEnvioMock implements WhatsAppAdapter {
  private static final Logger log = LoggerFactory.getLogger(WhatsappEnvioMock.class);

  @Override
  public boolean enviarWhatsApp(String telefono, String mensaje) {

    log.info(
        """
            [WHATSAPP SIMULADO]

            Telefono: {}
            Mensaje: {}
            """,
        telefono,
        mensaje);

    return true;
  }
}
