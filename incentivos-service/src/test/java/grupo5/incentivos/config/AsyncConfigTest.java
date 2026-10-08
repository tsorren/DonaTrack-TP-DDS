package grupo5.incentivos.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

  @Test
  void notificacionesTaskExecutor_deberiaConfigurarPoolCorrectamente() {
    AsyncConfig config = new AsyncConfig();
    Executor executor = config.notificacionesTaskExecutor(r -> r);

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
  void notificacionesTaskExecutor_deberiaAplicarElTaskDecoratorRecibido() throws Exception {
    AtomicBoolean decorado = new AtomicBoolean(false);
    TaskDecorator decorator =
        r -> {
          decorado.set(true);
          return r;
        };

    ThreadPoolTaskExecutor executor =
        (ThreadPoolTaskExecutor) new AsyncConfig().notificacionesTaskExecutor(decorator);
    try {
      CountDownLatch latch = new CountDownLatch(1);
      executor.execute(latch::countDown);

      assertTrue(latch.await(5, TimeUnit.SECONDS));
      assertTrue(decorado.get());
    } finally {
      executor.shutdown();
    }
  }
}
