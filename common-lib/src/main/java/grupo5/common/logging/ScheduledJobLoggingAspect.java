package grupo5.common.logging;

import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class ScheduledJobLoggingAspect {
  private static final Logger log = LoggerFactory.getLogger(ScheduledJobLoggingAspect.class);
  private final Tracer tracer;

  public ScheduledJobLoggingAspect(Tracer tracer) {
    this.tracer = tracer;
  }

  @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
  public Object traceScheduledJob(ProceedingJoinPoint joinPoint) throws Throwable {
    String jobName = joinPoint.getSignature().toShortString();
    log.info("[SCHEDULED-JOB-START] Starting execution of job: {}", jobName);

    ScopedSpan newSpan = tracer.startScopedSpan(jobName);
    try {
      Object result = joinPoint.proceed();
      log.info("[SCHEDULED-JOB-SUCCESS] Finished execution of job: {}", jobName);
      return result;
    } catch (Throwable t) {
      newSpan.error(t);
      throw new RuntimeException("Scheduled job failed: " + jobName, t);
    } finally {
      newSpan.end();
    }
  }
}
