package grupo5.donaciones.services;

import static org.mockito.Mockito.*;

import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.impl.PlanificacionNecesidadesService;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanificacionNecesidadesServiceTest {

  @Mock private INecesidadesRepository necesidadRepository;

  @InjectMocks private PlanificacionNecesidadesService planificacionNecesidadesService;

  @Test
  void testGenerarNuevosPeriodosParaNecesidadesRecurrentes() {
    NecesidadRecurrente necesidadRecurrente = mock(NecesidadRecurrente.class);

    when(necesidadRepository.findByActivaTrue())
        .thenReturn(Collections.singletonList(necesidadRecurrente));

    when(necesidadRecurrente.hayQueGenerarNuevo(any(LocalDate.class))).thenReturn(true);

    planificacionNecesidadesService.generarNuevosPeriodosParaNecesidadesRecurrentes();

    verify(necesidadRecurrente, times(1)).generarNuevoPeriodo();

    verify(necesidadRepository, times(1)).save(necesidadRecurrente);
  }
}
