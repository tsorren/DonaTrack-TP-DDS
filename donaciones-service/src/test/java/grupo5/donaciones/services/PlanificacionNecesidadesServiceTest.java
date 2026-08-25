package grupo5.donaciones.services;

import static org.mockito.Mockito.*;

import grupo5.donaciones.models.entities.necesidades.GestorNecesidades;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.impl.PlanificacionNecesidadesService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanificacionNecesidadesServiceTest {

  @Mock private INecesidadesRepository necesidadRepository;
  @Mock private GestorNecesidades gestorNecesidades;

  @InjectMocks private PlanificacionNecesidadesService planificacionNecesidadesService;

  @Test
  void generarNuevos_deberiaDelegar_AlGestor_YPersistirLasRetornadas() {
    NecesidadRecurrente necesidadRecurrente = mock(NecesidadRecurrente.class);
    when(necesidadRepository.findByActivaTrueAndSatisfechaFalseAndRecurrenteTrue())
        .thenReturn(Collections.singletonList(necesidadRecurrente));
    when(gestorNecesidades.generarNuevosPeriodos(any(), any(LocalDate.class)))
        .thenReturn(List.of(necesidadRecurrente));

    planificacionNecesidadesService.generarNuevosPeriodosParaNecesidadesRecurrentes();

    verify(gestorNecesidades, times(1)).generarNuevosPeriodos(any(), any(LocalDate.class));
    verify(necesidadRepository, times(1)).save(necesidadRecurrente);
  }

  @Test
  void generarNuevos_cuandoGestorNoRetornaNada_noDeberiaGuardar() {
    NecesidadRecurrente necesidadRecurrente = mock(NecesidadRecurrente.class);
    when(necesidadRepository.findByActivaTrueAndSatisfechaFalseAndRecurrenteTrue())
        .thenReturn(Collections.singletonList(necesidadRecurrente));
    when(gestorNecesidades.generarNuevosPeriodos(any(), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());

    planificacionNecesidadesService.generarNuevosPeriodosParaNecesidadesRecurrentes();

    verify(necesidadRepository, never()).save(any());
  }

  @Test
  void generarNuevos_deberiaGuardar_ExactamenteLasRetornadasPorElGestor() {
    NecesidadRecurrente necesidadRecurrente = mock(NecesidadRecurrente.class);
    when(necesidadRepository.findByActivaTrueAndSatisfechaFalseAndRecurrenteTrue())
        .thenReturn(Collections.singletonList(necesidadRecurrente));
    when(gestorNecesidades.generarNuevosPeriodos(any(), any(LocalDate.class)))
        .thenReturn(List.of(necesidadRecurrente));

    planificacionNecesidadesService.generarNuevosPeriodosParaNecesidadesRecurrentes();

    verify(necesidadRepository, times(1)).save(necesidadRecurrente);
    verify(necesidadRepository, never()).save(argThat(n -> n != necesidadRecurrente));
  }

  @Test
  void generarNuevos_conVariasNecesidades_soloGuardaLasRetornadasPorGestor() {
    NecesidadRecurrente necesidadA = mock(NecesidadRecurrente.class);
    NecesidadRecurrente necesidadB = mock(NecesidadRecurrente.class);
    NecesidadRecurrente necesidadC = mock(NecesidadRecurrente.class);
    when(necesidadRepository.findByActivaTrueAndSatisfechaFalseAndRecurrenteTrue())
        .thenReturn(
            List.of((Necesidad) necesidadA, (Necesidad) necesidadB, (Necesidad) necesidadC));
    when(gestorNecesidades.generarNuevosPeriodos(any(), any(LocalDate.class)))
        .thenReturn(List.of(necesidadA, necesidadC));

    planificacionNecesidadesService.generarNuevosPeriodosParaNecesidadesRecurrentes();

    verify(necesidadRepository, times(1)).save(necesidadA);
    verify(necesidadRepository, never()).save(necesidadB);
    verify(necesidadRepository, times(1)).save(necesidadC);
  }
}
