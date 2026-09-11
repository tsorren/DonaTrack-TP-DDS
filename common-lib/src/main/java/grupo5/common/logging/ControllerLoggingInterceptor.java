package grupo5.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class ControllerLoggingInterceptor implements HandlerInterceptor {
  private static final Logger log = LoggerFactory.getLogger(ControllerLoggingInterceptor.class);
  private static final String EVENT_TYPE = "eventType";
  private static final String HTTP_METHOD = "httpMethod";
  private static final String ENDPOINT = "endpoint";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (handler instanceof HandlerMethod handlerMethod) {
      String verb = request.getMethod();
      String path = request.getRequestURI();
      String packageName = handlerMethod.getBeanType().getPackageName();
      String className = handlerMethod.getBeanType().getSimpleName();
      String methodName = handlerMethod.getMethod().getName();

      MDC.put(EVENT_TYPE, "HTTP_IN");
      MDC.put(HTTP_METHOD, verb);
      MDC.put(ENDPOINT, path);

      try {
        log.info(
            "[CONTROLLER] [VERB: {}] [PATH: {}] [PACKAGE: {}] [CLASS: {}] [METHOD: {}] - Request received",
            verb,
            path,
            packageName,
            className,
            methodName);
      } finally {
        MDC.remove(EVENT_TYPE);
      }
    }
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    MDC.remove(HTTP_METHOD);
    MDC.remove(ENDPOINT);
    MDC.remove(EVENT_TYPE);
  }
}
