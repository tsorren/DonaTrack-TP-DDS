package grupo5.common.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class CommonAsyncAutoConfigurationTest {

  @Test
  void mdcTaskDecorator_deberiaRetornarInstanciaValida() {
    CommonAsyncAutoConfiguration config = new CommonAsyncAutoConfiguration();
    TaskDecorator decorator = config.mdcTaskDecorator();

    assertNotNull(decorator);
    assertInstanceOf(MdcTaskDecorator.class, decorator);
  }

  @Test
  void taskExecutor_deberiaConfigurarPoolCorrectamente() {
    CommonAsyncAutoConfiguration config = new CommonAsyncAutoConfiguration();
    Executor executor = config.taskExecutor(new MdcTaskDecorator());

    assertNotNull(executor);
    assertInstanceOf(ThreadPoolTaskExecutor.class, executor);

    ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
    assertEquals(4, pool.getCorePoolSize());
    assertEquals(20, pool.getMaxPoolSize());
    assertEquals(500, pool.getQueueCapacity());
    assertEquals("donatrack-async-", pool.getThreadNamePrefix());

    pool.shutdown();
  }
}
