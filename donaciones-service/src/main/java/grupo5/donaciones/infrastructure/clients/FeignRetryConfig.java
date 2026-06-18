package grupo5.donaciones.infrastructure.clients;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignRetryConfig {

  @Bean
  public Retryer feignRetryer() {
    return new Retryer.Default(100, 2000, 5);
  }
}
