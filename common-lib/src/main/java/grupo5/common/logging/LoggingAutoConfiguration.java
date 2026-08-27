package grupo5.common.logging;

import feign.RequestInterceptor;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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

  @Bean
  @ConditionalOnMissingBean
  public TraceResponseHeaderFilter traceResponseHeaderFilter() {
    return new TraceResponseHeaderFilter();
  }

  @Bean
  @ConditionalOnClass(RequestInterceptor.class)
  @ConditionalOnMissingBean
  public FeignTraceRequestInterceptor feignTraceRequestInterceptor() {
    return new FeignTraceRequestInterceptor();
  }

  @Configuration
  public static class LoggingWebMvcConfiguration implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(new ControllerLoggingInterceptor());
    }
  }
}
