package grupo5.common.testing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;

class DisabledIfDockerUnavailableConditionTest {

  @Test
  void evaluateExecutionCondition_cuandoDockerDisponible_deberiaHabilitar() {
    DisabledIfDockerUnavailableCondition condition =
        new DisabledIfDockerUnavailableCondition() {
          @Override
          boolean isDockerAvailable() {
            return true;
          }
        };

    ConditionEvaluationResult result = condition.evaluateExecutionCondition(null);

    assertFalse(result.isDisabled());
    assertTrue(result.getReason().orElse("").contains("disponible"));
  }

  @Test
  void evaluateExecutionCondition_cuandoDockerNoDisponible_deberiaDeshabilitar() {
    DisabledIfDockerUnavailableCondition condition =
        new DisabledIfDockerUnavailableCondition() {
          @Override
          boolean isDockerAvailable() {
            return false;
          }
        };

    ConditionEvaluationResult result = condition.evaluateExecutionCondition(null);

    assertTrue(result.isDisabled());
    assertTrue(result.getReason().orElse("").contains("[DEFERRED_NO_DOCKER]"));
  }

  @Test
  void evaluateExecutionCondition_cuandoLanzaExcepcion_deberiaDeshabilitarConMensaje() {
    DisabledIfDockerUnavailableCondition condition =
        new DisabledIfDockerUnavailableCondition() {
          @Override
          boolean isDockerAvailable() {
            throw new IllegalStateException("Socket cerrado");
          }
        };

    ConditionEvaluationResult result = condition.evaluateExecutionCondition(null);

    assertTrue(result.isDisabled());
    assertTrue(result.getReason().orElse("").contains("[DEFERRED_NO_DOCKER]"));
  }
}
