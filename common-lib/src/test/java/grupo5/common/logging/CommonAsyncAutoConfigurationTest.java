package grupo5.common.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
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
  void getAsyncExecutor_deberiaConfigurarPoolCorrectamente() {
    CommonAsyncAutoConfiguration config = new CommonAsyncAutoConfiguration();
    Executor executor = config.getAsyncExecutor();

    assertNotNull(executor);
    assertInstanceOf(ThreadPoolTaskExecutor.class, executor);

    ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
    assertEquals(4, pool.getCorePoolSize());
    assertEquals(20, pool.getMaxPoolSize());
    assertEquals(500, pool.getQueueCapacity());
    assertEquals("donatrack-async-", pool.getThreadNamePrefix());

    pool.shutdown();
  }

  @Test
  void getAsyncUncaughtExceptionHandler_deberiaRetornarLoggingHandler()
      throws NoSuchMethodException {
    CommonAsyncAutoConfiguration config = new CommonAsyncAutoConfiguration();
    AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();

    assertNotNull(handler);
    assertInstanceOf(LoggingAsyncUncaughtExceptionHandler.class, handler);

    Method dummyMethod = String.class.getMethod("toString");
    assertDoesNotThrow(
        () ->
            handler.handleUncaughtException(
                new RuntimeException("Test async error"), dummyMethod, "param1"));
  }
}
