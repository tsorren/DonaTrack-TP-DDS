package grupo5.notificaciones.infrastructure.adapters;

import grupo5.notificaciones.infrastructure.WhatsAppAdapter;
import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para despacho de mensajes vía WhatsApp. Aplica {@link CriterioFalloSimulado}
 * para admitir escenarios de fallo controlado.
 */
@Component
public class WhatsAppAdapterSimulado implements WhatsAppAdapter {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppAdapterSimulado.class);

  private final CriterioFalloSimulado criterioFallo;

  public WhatsAppAdapterSimulado(CriterioFalloSimulado criterioFallo) {
    this.criterioFallo = criterioFallo;
  }

  @Override
  public boolean enviarWhatsApp(String telefono, String mensaje) {
    if (criterioFallo.debeFallar(telefono, mensaje)) {
      log.warn(
          """
              [WHATSAPP SIMULADO FALLIDO]

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
            [WHATSAPP SIMULADO]

            Telefono: {}
            Mensaje: {}
            """,
        telefono,
        mensaje);

    return true;
  }
}
