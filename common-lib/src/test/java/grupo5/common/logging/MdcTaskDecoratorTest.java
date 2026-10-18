package grupo5.common.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcTaskDecoratorTest {

  private MdcTaskDecorator decorator;
  private ExecutorService executor;

  @BeforeEach
  void setUp() {
    decorator = new MdcTaskDecorator();
    executor = Executors.newSingleThreadExecutor();
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
    executor.shutdownNow();
  }

  @Test
  void decorate_deberiaPropagarMdcContextoAHiloDeTrabajo()
      throws ExecutionException, InterruptedException, TimeoutException {
    MDC.put("traceId", "trace-abc-123");
    MDC.put("customKey", "customValue");

    CompletableFuture<String> traceIdFuture = new CompletableFuture<>();
    CompletableFuture<String> customKeyFuture = new CompletableFuture<>();

    Runnable decorated =
        decorator.decorate(
            () -> {
              traceIdFuture.complete(MDC.get("traceId"));
              customKeyFuture.complete(MDC.get("customKey"));
            });

    executor.submit(decorated);

    assertEquals("trace-abc-123", traceIdFuture.get(5, TimeUnit.SECONDS));
    assertEquals("customValue", customKeyFuture.get(5, TimeUnit.SECONDS));
  }

  @Test
  void decorate_deberiaLimpiarMdcInclusoSiFallaLaTarea()
      throws ExecutionException, InterruptedException, TimeoutException {
    MDC.put("traceId", "trace-error-test");

    AtomicReference<String> mdcAfterError = new AtomicReference<>();
    CompletableFuture<Void> taskFinished = new CompletableFuture<>();

    Runnable throwingTask =
        () -> {
          throw new RuntimeException("Fallo intencional");
        };

    Runnable decorated = decorator.decorate(throwingTask);

    executor.submit(
        () -> {
          try {
            decorated.run();
          } catch (Exception ignored) {
            // Se ignora la excepción intencional
          } finally {
            mdcAfterError.set(MDC.get("traceId"));
            taskFinished.complete(null);
          }
        });

    taskFinished.get(5, TimeUnit.SECONDS);
    assertNull(mdcAfterError.get(), "El MDC debio haberse limpiado tras la excepcion en finally");
  }

  @Test
  void decorate_deberiaManejarContextoMdcNuloSinErrores()
      throws ExecutionException, InterruptedException, TimeoutException {
    MDC.clear();

    CompletableFuture<String> resultFuture = new CompletableFuture<>();

    Runnable decorated =
        decorator.decorate(
            () -> {
              resultFuture.complete(MDC.get("traceId"));
            });

    executor.submit(decorated);

    assertNull(resultFuture.get(5, TimeUnit.SECONDS));
  }

  @Test
  void decorate_noDeberiaAfectarContextoDelHiloPadre()
      throws ExecutionException, InterruptedException, TimeoutException {
    MDC.put("traceId", "parent-trace-id");

    CompletableFuture<Void> done = new CompletableFuture<>();

    Runnable decorated =
        decorator.decorate(
            () -> {
              MDC.put("traceId", "child-mutated-trace-id");
              done.complete(null);
            });

    executor.submit(decorated);
    done.get(5, TimeUnit.SECONDS);

    assertEquals(
        "parent-trace-id",
        MDC.get("traceId"),
        "El contexto MDC del hilo padre no debe verse afectado por el hilo hijo");
  }
}
