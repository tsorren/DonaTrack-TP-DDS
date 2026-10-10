package grupo5.common.logging;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@AutoConfiguration(after = LoggingAutoConfiguration.class)
@ConditionalOnClass(EnableAsync.class)
public class CommonAsyncAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(TaskDecorator.class)
  public TaskDecorator mdcTaskDecorator() {
    return new MdcTaskDecorator();
  }

  @Bean(name = "taskExecutor")
  @ConditionalOnMissingBean(name = "taskExecutor")
  public Executor taskExecutor(TaskDecorator taskDecorator) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("donatrack-async-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.setTaskDecorator(taskDecorator);
    executor.initialize();
    return executor;
  }
}
