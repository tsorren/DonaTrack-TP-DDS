package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.impl.PlanificacionNecesidadesService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
  void generarNuevos_cuandoCorrespondeRenovar_deberiaPersistirNecesidad() {
    NecesidadRecurrente necesidadRecurrente = mock(NecesidadRecurrente.class);
    when(necesidadRepository.findByActivaTrueAndRecurrenteTrue())
        .thenReturn(Collections.singletonList(necesidadRecurrente));
    when(necesidadRecurrente.renovarPeriodoSiCorresponde(any(LocalDate.class))).thenReturn(true);

    planificacionNecesidadesService.generarNuevosPeriodosParaNecesidadesRecurrentes();

    verify(necesidadRepository, times(1)).save(necesidadRecurrente);
  }

  @Test
  void generarNuevos_cuandoNoCorrespondeRenovar_noDeberiaGuardar() {
    NecesidadRecurrente necesidadRecurrente = mock(NecesidadRecurrente.class);
    when(necesidadRepository.findByActivaTrueAndRecurrenteTrue())
        .thenReturn(Collections.singletonList(necesidadRecurrente));
    when(necesidadRecurrente.renovarPeriodoSiCorresponde(any(LocalDate.class))).thenReturn(false);

    planificacionNecesidadesService.generarNuevosPeriodosParaNecesidadesRecurrentes();

    verify(necesidadRepository, never()).save(any());
  }

  @Test
  void generarNuevos_conVariasNecesidades_soloGuardaLasQueFueronRenovadas() {
    NecesidadRecurrente necesidadA = mock(NecesidadRecurrente.class);
    NecesidadRecurrente necesidadB = mock(NecesidadRecurrente.class);
    NecesidadRecurrente necesidadC = mock(NecesidadRecurrente.class);
    when(necesidadRepository.findByActivaTrueAndRecurrenteTrue())
        .thenReturn(
            List.of((Necesidad) necesidadA, (Necesidad) necesidadB, (Necesidad) necesidadC));
    when(necesidadA.renovarPeriodoSiCorresponde(any(LocalDate.class))).thenReturn(true);
    when(necesidadB.renovarPeriodoSiCorresponde(any(LocalDate.class))).thenReturn(false);
    when(necesidadC.renovarPeriodoSiCorresponde(any(LocalDate.class))).thenReturn(true);

    planificacionNecesidadesService.generarNuevosPeriodosParaNecesidadesRecurrentes();

    verify(necesidadRepository, times(1)).save(necesidadA);
    verify(necesidadRepository, never()).save(necesidadB);
    verify(necesidadRepository, times(1)).save(necesidadC);
  }

  @Test
  void generarNuevos_conNecesidadRecurrenteSatisfecha_debeRenovarPeriodo() {
    UUID subcategoriaId = UUID.randomUUID();
    LocalDate fechaInicio = LocalDate.now().minusWeeks(2);
    java.time.Period periodo = java.time.Period.ofWeeks(1);

    NecesidadRecurrente recurrente =
        new NecesidadRecurrente(subcategoriaId, 10, "Fideos semanales", periodo, fechaInicio);

    // Periodo inicial vencio hace 1 semana y fue satisfecho
    when(necesidadRepository.findByActivaTrueAndRecurrenteTrue()).thenReturn(List.of(recurrente));

    planificacionNecesidadesService.generarNuevosPeriodosParaNecesidadesRecurrentes();

    verify(necesidadRepository, times(1)).save(recurrente);
    assertEquals(2, recurrente.getPeriodos().size());
  }
}
