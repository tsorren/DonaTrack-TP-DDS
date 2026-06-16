package grupo5.notificaciones.infrastructure.mockEnvios;

import grupo5.notificaciones.infrastructure.CorreoAdapter;

public class CorreoEnvioMock implements CorreoAdapter {

  @Override
  public boolean enviarMail(String destinatario, String mensaje) {

    System.out.println(
        """
            [EMAIL SIMULADO]

            Destinatario: %s
            Mensaje: %s
            """
            .formatted(destinatario, mensaje));

    return true;
  }
}
