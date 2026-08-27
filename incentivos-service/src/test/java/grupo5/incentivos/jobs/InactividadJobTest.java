package grupo5.incentivos.jobs;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import grupo5.incentivos.services.IInactividadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InactividadJobTest {

  @Mock private IInactividadService service;

  @InjectMocks private InactividadJob job;

  @Test
  void ejecutar_deberiaInvocarProcesarInactividad() {
    job.ejecutar();

    verify(service, times(1)).procesarInactividad();
  }
}
