package grupo5.common.logging;

import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
public class LoggingAutoConfiguration {

  @Bean
  public ServiceLoggingAspect serviceLoggingAspect() {
    return new ServiceLoggingAspect();
  }

  @Bean
  @ConditionalOnBean(Tracer.class)
  public ScheduledJobLoggingAspect scheduledJobLoggingAspect(Tracer tracer) {
    return new ScheduledJobLoggingAspect(tracer);
  }

  @Configuration
  public static class LoggingWebMvcConfiguration implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(new ControllerLoggingInterceptor());
    }
  }
}
