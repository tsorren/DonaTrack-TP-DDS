package grupo5.donaciones.services;

import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.comunicaciones.NuevaEntregaRequest;
import grupo5.donaciones.infrastructure.clients.LogisticaFeignClient;
import grupo5.donaciones.services.impl.LogisticaAsyncService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogisticaAsyncServiceTest {

  @Mock private LogisticaFeignClient client;

  @InjectMocks private LogisticaAsyncService service;

  @Test
  void registrarEntregaPendiente_deberiaInvocarCliente_CuandoNoHayErrores() {
    NuevaEntregaRequest request =
        new NuevaEntregaRequest(UUID.randomUUID(), UUID.randomUUID(), null, 12.5, 0.3);

    service.registrarEntregaPendiente(request);

    verify(client, times(1)).registrarEntregaPendiente(request);
  }

  @Test
  void registrarEntregaPendiente_deberiaCapturarExcepcionYNoPropagarla_CuandoClienteFalla() {
    NuevaEntregaRequest request =
        new NuevaEntregaRequest(UUID.randomUUID(), UUID.randomUUID(), null, 12.5, 0.3);
    doThrow(new RuntimeException("Error de conexión"))
        .when(client)
        .registrarEntregaPendiente(request);

    // No debe lanzar excepción
    service.registrarEntregaPendiente(request);

    verify(client, times(1)).registrarEntregaPendiente(request);
  }
}
