package grupo5.notificaciones.infrastructure.adapters.politicas;

import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Implementación de {@link CriterioFalloSimulado} que inspecciona el destinatario o mensaje en
 * busca de sentinelas predefinidos o datos malformados para disparar fallos determinísticos.
 */
@Component
public class CriterioFalloPorSentinela implements CriterioFalloSimulado {

  private static final String SENTINELA_FALLO = "fallo";
  private static final String SENTINELA_ERROR = "error";
  private static final String SENTINELA_TELEFONO_INVALIDO = "0000000000";

  @Override
  public boolean debeFallar(String destinatario, String mensaje) {
    if (destinatario == null || destinatario.isBlank()) {
      return true;
    }
    if (mensaje == null || mensaje.isBlank()) {
      return true;
    }

    String destinoNormalizado = destinatario.toLowerCase(Locale.ROOT);
    return destinoNormalizado.contains(SENTINELA_FALLO)
        || destinoNormalizado.contains(SENTINELA_ERROR)
        || destinoNormalizado.contains(SENTINELA_TELEFONO_INVALIDO);
  }
}
