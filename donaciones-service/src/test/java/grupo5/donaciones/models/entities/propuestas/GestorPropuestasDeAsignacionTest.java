package grupo5.donaciones.models.entities.propuestas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.models.algoritmos.AlgoritmoAsignacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GestorPropuestasDeAsignacionTest {

  private AlgoritmoAsignacion algoritmo1Mock;
  private AlgoritmoAsignacion algoritmo2Mock;
  private GestorPropuestasDeAsignacion gestor;

  @BeforeEach
  void setUp() {
    algoritmo1Mock = mock(AlgoritmoAsignacion.class);
    algoritmo2Mock = mock(AlgoritmoAsignacion.class);
    gestor = new GestorPropuestasDeAsignacion(List.of(algoritmo1Mock, algoritmo2Mock));
  }

  @Test
  void generarPropuestas_cuandoAmbosAlgoritmosCoincidenEnNecesidad_deberiaConsolidarInterseccion() {
    UUID necesidad1Id = UUID.randomUUID();
    UUID necesidad2Id = UUID.randomUUID();

    Propuesta prop1 = new Propuesta();
    prop1.asociarNecesidad(necesidad1Id);

    Propuesta prop2 = new Propuesta();
    prop2.asociarNecesidad(necesidad2Id);

    Propuesta prop3 = new Propuesta();
    prop3.asociarNecesidad(necesidad1Id);

    when(algoritmo1Mock.ejecutar(anyList(), anyList())).thenReturn(List.of(prop1, prop2));
    when(algoritmo2Mock.ejecutar(anyList(), anyList())).thenReturn(List.of(prop3));

    List<Necesidad> necesidades = List.of(mock(Necesidad.class));
    List<DonacionIndependiente> donaciones = List.of(mock(DonacionIndependiente.class));

    List<Propuesta> resultado = gestor.generarPropuestas(necesidades, donaciones);

    assertEquals(1, resultado.size());
    assertSame(prop3, resultado.getFirst());
    assertEquals(necesidad1Id, resultado.getFirst().getNecesidadQueSatisfaceId());
  }

  @Test
  void generarPropuestas_cuandoNoHayCoincidencias_deberiaUnirTodasLasPropuestas() {
    UUID necesidad1Id = UUID.randomUUID();
    UUID necesidad2Id = UUID.randomUUID();

    Propuesta prop1 = new Propuesta();
    prop1.asociarNecesidad(necesidad1Id);

    Propuesta prop2 = new Propuesta();
    prop2.asociarNecesidad(necesidad2Id);

    when(algoritmo1Mock.ejecutar(anyList(), anyList())).thenReturn(List.of(prop1));
    when(algoritmo2Mock.ejecutar(anyList(), anyList())).thenReturn(List.of(prop2));

    List<Propuesta> resultado = gestor.generarPropuestas(List.of(), List.of());

    assertEquals(2, resultado.size());
    assertTrue(resultado.contains(prop1));
    assertTrue(resultado.contains(prop2));
  }

  @Test
  void generarPropuestas_conListasVacias_retornaVacio() {
    when(algoritmo1Mock.ejecutar(anyList(), anyList())).thenReturn(List.of());
    when(algoritmo2Mock.ejecutar(anyList(), anyList())).thenReturn(List.of());

    List<Propuesta> resultado = gestor.generarPropuestas(List.of(), List.of());

    assertTrue(resultado.isEmpty());
  }
}
