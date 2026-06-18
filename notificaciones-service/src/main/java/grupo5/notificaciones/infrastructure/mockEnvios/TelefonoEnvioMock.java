package grupo5.notificaciones.infrastructure.mockEnvios;

import grupo5.notificaciones.infrastructure.TelefonoAdapter;
import org.springframework.stereotype.Component;

@Component
public class TelefonoEnvioMock implements TelefonoAdapter {

  @Override
  public boolean enviarSms(String telefono, String mensaje) {

    System.out.println(
        """
            [SMS SIMULADO]

            Telefono: %s
            Mensaje: %s
            """
            .formatted(telefono, mensaje));

    return true;
  }
}
