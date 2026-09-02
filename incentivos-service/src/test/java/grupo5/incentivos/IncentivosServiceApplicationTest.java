package grupo5.incentivos;

import grupo5.incentivos.infrastructure.clients.NotificacionesFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class IncentivosServiceApplicationTest {
  @MockitoBean private NotificacionesFeignClient notificacionesFeignClient;

  @Test
  void contextLoads() {}
}
