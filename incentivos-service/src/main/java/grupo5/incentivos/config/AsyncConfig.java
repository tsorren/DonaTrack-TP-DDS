package grupo5.incentivos.config;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "notificacionesTaskExecutor")
  public Executor notificacionesTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("async-notif-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setTaskDecorator(
        runnable -> {
          Map<String, String> contextMap = MDC.getCopyOfContextMap();
          return () -> {
            try {
              if (contextMap != null) {
                MDC.setContextMap(contextMap);
              }
              runnable.run();
            } finally {
              MDC.clear();
            }
          };
        });
    executor.initialize();
    return executor;
  }
}
