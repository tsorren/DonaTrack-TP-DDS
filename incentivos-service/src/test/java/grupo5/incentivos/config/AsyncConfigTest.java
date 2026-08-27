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
}
