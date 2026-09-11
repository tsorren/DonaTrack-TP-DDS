package grupo5.common.logging;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

/**
 * TaskDecorator que propaga el contexto MDC (incluyendo traceId) desde el hilo invocante hacia el
 * hilo del pool asíncrono.
 */
@Component
public class MdcTaskDecorator implements TaskDecorator {

  @Override
  public Runnable decorate(Runnable runnable) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    return () -> {
      try {
        if (contextMap != null) {
          MDC.setContextMap(contextMap);
        } else {
          MDC.clear();
        }
        runnable.run();
      } finally {
        MDC.clear();
      }
    };
  }
}
