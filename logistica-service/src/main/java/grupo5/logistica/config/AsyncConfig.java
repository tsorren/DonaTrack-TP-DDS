package grupo5.logistica.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "proveedorExternoExecutor")
  public Executor proveedorExternoExecutor(TaskDecorator taskDecorator) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("proveedor-ruteo-");
    // CallerRunsPolicy: cuando la cola llega a 50 elementos, el hilo invocante (el scheduler)
    // ejecuta la tarea directamente en lugar de descartarla. Evita perder solicitudes de
    // planificacion a cambio de frenar temporalmente al scheduler, lo cual es aceptable porque
    // el scheduler corre en horarios de baja carga y la perdida silenciosa de lotes seria peor.
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    // TaskDecorator: inyectado desde common-lib (MdcTaskDecorator) para que el traceId no se
    // pierda en los logs del hilo de ruteo asincrono.
    executor.setTaskDecorator(taskDecorator);
    executor.initialize();
    return executor;
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
