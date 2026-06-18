package grupo5.notificaciones.infrastructure.mockEnvios;

import grupo5.notificaciones.infrastructure.WhatsAppAdapter;
import org.springframework.stereotype.Component;

@Component
public class WhatsappEnvioMock implements WhatsAppAdapter {

  @Override
  public boolean enviarWhatsApp(String telefono, String mensaje) {

    System.out.println(
        """
            [WHATSAPP SIMULADO]

            Telefono: %s
            Mensaje: %s
            """
            .formatted(telefono, mensaje));

    return true;
  }
}
