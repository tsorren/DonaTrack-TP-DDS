package grupo5.donaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "grupo5")
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class DonacionesServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(DonacionesServiceApplication.class, args);
  }
}
