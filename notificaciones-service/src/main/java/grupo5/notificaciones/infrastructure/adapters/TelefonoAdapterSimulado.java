package grupo5.notificaciones.infrastructure.adapters;

import grupo5.notificaciones.infrastructure.TelefonoAdapter;
import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para despacho de SMS telefónicos. Aplica {@link CriterioFalloSimulado} para
 * admitir escenarios de fallo controlado.
 */
@Component
public class TelefonoAdapterSimulado implements TelefonoAdapter {

  private static final Logger log = LoggerFactory.getLogger(TelefonoAdapterSimulado.class);

  private final CriterioFalloSimulado criterioFallo;

  public TelefonoAdapterSimulado(CriterioFalloSimulado criterioFallo) {
    this.criterioFallo = criterioFallo;
  }

  @Override
  public boolean enviarSms(String telefono, String mensaje) {
    if (criterioFallo.debeFallar(telefono, mensaje)) {
      log.warn(
          """
              [SMS SIMULADO FALLIDO]

              Telefono: {}
              Mensaje: {}
              Motivo: Fallo simulado por criterio de política
              """,
          telefono,
          mensaje);
      return false;
    }

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
