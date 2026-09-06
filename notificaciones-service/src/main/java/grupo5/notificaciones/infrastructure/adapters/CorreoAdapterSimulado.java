package grupo5.notificaciones.infrastructure.adapters;

import grupo5.notificaciones.infrastructure.CorreoAdapter;
import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para despacho de correos electrónicos. Aplica {@link CriterioFalloSimulado}
 * para admitir escenarios de fallo controlado.
 */
@Component
public class CorreoAdapterSimulado implements CorreoAdapter {

  private static final Logger log = LoggerFactory.getLogger(CorreoAdapterSimulado.class);

  private final CriterioFalloSimulado criterioFallo;

  public CorreoAdapterSimulado(CriterioFalloSimulado criterioFallo) {
    this.criterioFallo = criterioFallo;
  }

  @Override
  public boolean enviarMail(String destinatario, String mensaje) {
    if (criterioFallo.debeFallar(destinatario, mensaje)) {
      log.warn(
          """
              [EMAIL SIMULADO FALLIDO]

              Destinatario: {}
              Mensaje: {}
              Motivo: Fallo simulado por criterio de política
              """,
          destinatario,
          mensaje);
      return false;
    }

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
