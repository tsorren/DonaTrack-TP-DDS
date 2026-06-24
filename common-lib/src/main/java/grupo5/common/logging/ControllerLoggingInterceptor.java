package grupo5.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class ControllerLoggingInterceptor implements HandlerInterceptor {
  private static final Logger log = LoggerFactory.getLogger(ControllerLoggingInterceptor.class);

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (handler instanceof HandlerMethod handlerMethod) {
      String verb = request.getMethod();
      String path = request.getRequestURI();
      String packageName = handlerMethod.getBeanType().getPackageName();
      String className = handlerMethod.getBeanType().getSimpleName();
      String methodName = handlerMethod.getMethod().getName();
      log.info(
          "[CONTROLLER] [VERB: {}] [PATH: {}] [PACKAGE: {}] [CLASS: {}] [METHOD: {}] - Request received",
          verb,
          path,
          packageName,
          className,
          methodName);
    }
    return true;
  }
}
