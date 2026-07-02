package grupo5.logistica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "grupo5")
@EnableScheduling
@EnableFeignClients(basePackages = "grupo5.logistica.infrastructure.clients")
public class LogisticaServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(LogisticaServiceApplication.class, args);
  }
}
