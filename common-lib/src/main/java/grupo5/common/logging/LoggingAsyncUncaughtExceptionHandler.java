package grupo5.common.logging;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

/**
 * Manejador global para excepciones no capturadas en métodos @Async void. Preserva el contexto MDC
 * y registra el error con formato estructurado [ASYNC-ERROR].
 */
public class LoggingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

  private static final Logger log =
      LoggerFactory.getLogger(LoggingAsyncUncaughtExceptionHandler.class);

  @Override
  public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
    if (log.isErrorEnabled()) {
      log.error(
          "[ASYNC-ERROR] Excepción no capturada en método asíncrono '{}' de clase '{}' con parámetros"
              + " {}: {}",
          method.getName(),
          method.getDeclaringClass().getSimpleName(),
          Arrays.toString(params),
          throwable.getMessage(),
          throwable);
    }
  }
}
