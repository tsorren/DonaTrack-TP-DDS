package grupo5.donaciones.schedulers;

import static org.mockito.Mockito.*;

import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
import grupo5.donaciones.models.repositories.impl.NecesidadRecurrenteRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanificadorDeNecesidadesTest {

  private NecesidadRecurrenteRepository necesidadRepositoryMock;
  private PlanificadorDeNecesidades planificador;

  @BeforeEach
  void setUp() {
    necesidadRepositoryMock = mock(NecesidadRecurrenteRepository.class);
    planificador = new PlanificadorDeNecesidades(necesidadRepositoryMock);
  }

  @Test
  void generarNuevosPeriodos_deberiaHacerNada_CuandoNoHayNecesidadesActivas() {
    when(necesidadRepositoryMock.findByActivaTrue()).thenReturn(Collections.emptyList());

    planificador.generarNuevosPeriodos();

    verify(necesidadRepositoryMock, times(1)).findByActivaTrue();
    verify(necesidadRepositoryMock, never()).save(any());
  }

  @Test
  void generarNuevosPeriodos_deberiaCrearPeriodoYGuardar_CuandoHayQueGenerar() {
    NecesidadRecurrente recurrenteMock = mock(NecesidadRecurrente.class);
    PeriodoNecesidad periodoActualMock = mock(PeriodoNecesidad.class);

    when(necesidadRepositoryMock.findByActivaTrue()).thenReturn(List.of(recurrenteMock));
    when(recurrenteMock.hayQueGenerarNuevo(any())).thenReturn(true);
    when(recurrenteMock.obtenerPeriodoActual()).thenReturn(periodoActualMock);

    planificador.generarNuevosPeriodos();

    verify(periodoActualMock, times(1)).finalizo();
    verify(recurrenteMock, times(1)).generarNuevoPeriodo();
    verify(necesidadRepositoryMock, times(1)).save(recurrenteMock);
  }

  @Test
  void generarNuevosPeriodos_deberiaNoCrearPeriodo_CuandoNoHayQueGenerar() {
    NecesidadRecurrente recurrenteMock = mock(NecesidadRecurrente.class);

    when(necesidadRepositoryMock.findByActivaTrue()).thenReturn(List.of(recurrenteMock));
    when(recurrenteMock.hayQueGenerarNuevo(any())).thenReturn(false);

    planificador.generarNuevosPeriodos();

    verify(recurrenteMock, never()).generarNuevoPeriodo();
    verify(necesidadRepositoryMock, never()).save(recurrenteMock);
  }
}
