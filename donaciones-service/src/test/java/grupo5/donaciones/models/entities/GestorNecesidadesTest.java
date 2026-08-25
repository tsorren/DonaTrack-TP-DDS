package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.models.entities.necesidades.GestorNecesidades;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GestorNecesidadesTest {

  private GestorNecesidades gestor;
  private final LocalDate HOY = LocalDate.of(2026, 6, 9);

  @BeforeEach
  void setUp() {
    gestor = new GestorNecesidades();
  }

  @Test
  void generarNuevosPeriodos_cuandoNingunaNecesita_retornaListaVacia() {
    NecesidadRecurrente necesidad = mock(NecesidadRecurrente.class);
    when(necesidad.hayQueGenerarNuevo(HOY)).thenReturn(false);

    List<NecesidadRecurrente> modificadas = gestor.generarNuevosPeriodos(List.of(necesidad), HOY);

    assertTrue(modificadas.isEmpty());
    verify(necesidad, never()).generarNuevoPeriodo();
  }

  @Test
  void generarNuevosPeriodos_cuandoNecesitaYTienePeriodoActual_deberiaFinalizarYGenerarNuevo() {
    NecesidadRecurrente necesidad = mock(NecesidadRecurrente.class);
    PeriodoNecesidad periodoActual = mock(PeriodoNecesidad.class);
    when(necesidad.hayQueGenerarNuevo(HOY)).thenReturn(true);
    when(necesidad.obtenerPeriodoActual()).thenReturn(periodoActual);

    List<NecesidadRecurrente> modificadas = gestor.generarNuevosPeriodos(List.of(necesidad), HOY);

    verify(periodoActual, times(1)).finalizo();
    verify(necesidad, times(1)).generarNuevoPeriodo();
    assertEquals(1, modificadas.size());
    assertSame(necesidad, modificadas.get(0));
  }

  @Test
  void generarNuevosPeriodos_cuandoNecesitaYSinPeriodoActual_deberiaGenerarSinFinalizar() {
    NecesidadRecurrente necesidad = mock(NecesidadRecurrente.class);
    when(necesidad.hayQueGenerarNuevo(HOY)).thenReturn(true);
    when(necesidad.obtenerPeriodoActual()).thenReturn(null);

    List<NecesidadRecurrente> modificadas = gestor.generarNuevosPeriodos(List.of(necesidad), HOY);

    verify(necesidad, times(1)).generarNuevoPeriodo();
    assertEquals(1, modificadas.size());
  }

  @Test
  void generarNuevosPeriodos_conVariasNecesidades_soloRetornaLasModificadas() {
    NecesidadRecurrente necesidadA = mock(NecesidadRecurrente.class);
    NecesidadRecurrente necesidadB = mock(NecesidadRecurrente.class);
    NecesidadRecurrente necesidadC = mock(NecesidadRecurrente.class);
    when(necesidadA.hayQueGenerarNuevo(HOY)).thenReturn(true);
    when(necesidadB.hayQueGenerarNuevo(HOY)).thenReturn(false);
    when(necesidadC.hayQueGenerarNuevo(HOY)).thenReturn(true);
    when(necesidadA.obtenerPeriodoActual()).thenReturn(null);
    when(necesidadC.obtenerPeriodoActual()).thenReturn(null);

    List<NecesidadRecurrente> modificadas =
        gestor.generarNuevosPeriodos(List.of(necesidadA, necesidadB, necesidadC), HOY);

    verify(necesidadA, times(1)).generarNuevoPeriodo();
    verify(necesidadB, never()).generarNuevoPeriodo();
    verify(necesidadC, times(1)).generarNuevoPeriodo();
    assertEquals(2, modificadas.size());
    assertTrue(modificadas.contains(necesidadA));
    assertFalse(modificadas.contains(necesidadB));
    assertTrue(modificadas.contains(necesidadC));
  }
}
