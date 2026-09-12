package grupo5.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class NotificacionesServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(NotificacionesServiceApplication.class, args);
  }
}
