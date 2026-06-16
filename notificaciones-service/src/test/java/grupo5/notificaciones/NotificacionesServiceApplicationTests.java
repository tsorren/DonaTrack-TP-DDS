package grupo5.notificaciones;

import grupo5.notificaciones.models.repositories.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class NotificacionesServiceApplicationTests {

  @MockitoBean private NotificacionRepository notificacionRepository;

  @Test
  void contextLoads() {}
}
