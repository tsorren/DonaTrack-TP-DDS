package grupo5.logistica.config;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.MDC;
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
  public Executor proveedorExternoExecutor() {
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
    // TaskDecorator: copia el contexto MDC del hilo del scheduler al hilo async de
    // ProveedorExternoPlanificacionSimulado, para que el traceId de la solicitud HTTP que
    // disparo la planificacion no se pierda en los logs del hilo de ruteo.
    executor.setTaskDecorator(mdcPropagatingDecorator());
    executor.initialize();
    return executor;
  }

  private static TaskDecorator mdcPropagatingDecorator() {
    return runnable -> {
      Map<String, String> mdcContext = MDC.getCopyOfContextMap();
      return () -> {
        try {
          if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
          }
          runnable.run();
        } finally {
          MDC.clear();
        }
      };
    };
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
