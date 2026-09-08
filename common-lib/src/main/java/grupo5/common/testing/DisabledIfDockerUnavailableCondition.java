package grupo5.common.testing;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

/**
 * Condición de ejecución que verifica si el demonio/socket de Docker responde. En caso negativo o
 * ante errores de conexión, desactiva la prueba con {@code [DEFERRED_NO_DOCKER]}.
 */
public class DisabledIfDockerUnavailableCondition implements ExecutionCondition {

  private static final Logger log =
      LoggerFactory.getLogger(DisabledIfDockerUnavailableCondition.class);

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    try {
      if (isDockerAvailable()) {
        return ConditionEvaluationResult.enabled("Docker está disponible");
      }
      return ConditionEvaluationResult.disabled(
          "[DEFERRED_NO_DOCKER] Docker socket no responde; ejecución postergada");
    } catch (Exception t) {
      log.warn("Verificación de Docker falló: {}", t.getMessage());
      return ConditionEvaluationResult.disabled(
          "[DEFERRED_NO_DOCKER] Docker no disponible: " + t.getMessage());
    }
  }

  boolean isDockerAvailable() {
    return DockerClientFactory.instance().isDockerAvailable();
  }
}
