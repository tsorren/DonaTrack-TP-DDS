package grupo5.common.logging;

import brave.propagation.Propagation;
import feign.RequestInterceptor;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.config.BeanPostProcessor;
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

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(Tracer.class)
  public static class TracingLoggingConfiguration {
    @Bean
    @ConditionalOnBean(Tracer.class)
    public ScheduledJobLoggingAspect scheduledJobLoggingAspect(Tracer tracer) {
      return new ScheduledJobLoggingAspect(tracer);
    }
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(Propagation.Factory.class)
  public static class XTraceIdPropagationConfiguration {
    /** Envuelve la propagación de Brave para que el span del servidor adopte el X-Trace-Id. */
    @Bean
    public static BeanPostProcessor xTraceIdPropagationPostProcessor() {
      return new BeanPostProcessor() {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
          if (bean instanceof Propagation.Factory factory
              && !(bean instanceof XTraceIdPropagationFactory)) {
            return new XTraceIdPropagationFactory(factory);
          }
          return bean;
        }
      };
    }
  }

  @Bean
  @ConditionalOnMissingBean(TraceResponseHeaderFilter.class)
  public TraceResponseHeaderFilter traceResponseHeaderFilter() {
    return new TraceResponseHeaderFilter();
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(RequestInterceptor.class)
  public static class FeignLoggingConfiguration {
    @Bean
    @ConditionalOnMissingBean(FeignTraceRequestInterceptor.class)
    public FeignTraceRequestInterceptor feignTraceRequestInterceptor() {
      return new FeignTraceRequestInterceptor();
    }
  }

  @Configuration
  public static class LoggingWebMvcConfiguration implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(new ControllerLoggingInterceptor());
    }
  }
}
