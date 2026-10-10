package grupo5.incentivos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "grupo5")
@EnableScheduling
@EnableFeignClients
public class IncentivosServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(IncentivosServiceApplication.class, args);
  }
}
