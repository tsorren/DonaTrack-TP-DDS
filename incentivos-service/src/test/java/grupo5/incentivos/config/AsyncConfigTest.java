package grupo5.incentivos.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

  @Test
  void notificacionesTaskExecutor_deberiaConfigurarPoolCorrectamente() {
    AsyncConfig config = new AsyncConfig();
    Executor executor = config.notificacionesTaskExecutor();

    assertNotNull(executor);
    assertInstanceOf(ThreadPoolTaskExecutor.class, executor);

    ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
    assertEquals(2, pool.getCorePoolSize());
    assertEquals(10, pool.getMaxPoolSize());
    assertEquals(500, pool.getQueueCapacity());
    assertEquals("async-notif-", pool.getThreadNamePrefix());

    pool.shutdown();
  }

  @Test
  void notificacionesTaskExecutor_deberiaPropagarMdcContextoAHiloDeTrabajo() throws Exception {
    AsyncConfig config = new AsyncConfig();
    ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.notificacionesTaskExecutor();

    org.slf4j.MDC.put("traceId", "trace-12345");
    java.util.concurrent.CompletableFuture<String> future =
        new java.util.concurrent.CompletableFuture<>();

    executor.execute(
        () -> {
          String traceIdEnWorker = org.slf4j.MDC.get("traceId");
          future.complete(traceIdEnWorker);
        });

    String traceIdCapturado = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
    org.slf4j.MDC.clear();

    assertEquals("trace-12345", traceIdCapturado);
    executor.shutdown();
  }
}
