package grupo5.common.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class ServiceLoggingAspect {
  private static final Logger log = LoggerFactory.getLogger(ServiceLoggingAspect.class);

  @AfterReturning(
      pointcut =
          "within(@org.springframework.stereotype.Service *) || within(grupo5..services..*) || within(grupo5..services.impl..*)",
      returning = "result")
  public void logServiceMethodSuccess(JoinPoint joinPoint, Object result) {
    Object target = joinPoint.getTarget();
    String className = target != null ? target.getClass().getSimpleName() : "Unknown";
    String packageName = target != null ? target.getClass().getPackageName() : "Unknown";
    String methodName = joinPoint.getSignature().getName();
    log.info(
        "[SERVICE-SUCCESS] [PACKAGE: {}] [CLASS: {}] [METHOD: {}] - Completed successfully",
        packageName,
        className,
        methodName);
  }

  @AfterThrowing(
      pointcut =
          "within(@org.springframework.stereotype.Service *) || within(grupo5..services..*) || within(grupo5..services.impl..*)",
      throwing = "ex")
  public void logServiceMethodError(JoinPoint joinPoint, Throwable ex) {
    Object target = joinPoint.getTarget();
    String className = target != null ? target.getClass().getSimpleName() : "Unknown";
    String packageName = target != null ? target.getClass().getPackageName() : "Unknown";
    String methodName = joinPoint.getSignature().getName();
    log.error(
        "[SERVICE-ERROR] [PACKAGE: {}] [CLASS: {}] [METHOD: {}] - Failed with exception: {}",
        packageName,
        className,
        methodName,
        ex.getMessage());
  }
}
