package grupo5.logistica.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "proveedorExternoExecutor")
  public Executor proveedorExternoExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("proveedor-ruteo-");
    executor.initialize();
    return executor;
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
