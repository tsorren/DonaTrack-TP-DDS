package grupo5.donaciones.models.entities.propuestas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.models.algoritmos.AlgoritmoAsignacion;
import grupo5.donaciones.models.algoritmos.DatosDeAsignacion;
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
    when(algoritmo1Mock.tipo()).thenReturn(TipoAlgoritmo.COMPATIBILIDAD_SEMANTICA);
    when(algoritmo2Mock.tipo()).thenReturn(TipoAlgoritmo.PRIORIDAD_SUBATENDIDOS);
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

    when(algoritmo1Mock.ejecutar(anyList(), anyList(), any())).thenReturn(List.of(prop1, prop2));
    when(algoritmo2Mock.ejecutar(anyList(), anyList(), any())).thenReturn(List.of(prop3));

    List<Necesidad> necesidades = List.of(mock(Necesidad.class));
    List<DonacionIndependiente> donaciones = List.of(mock(DonacionIndependiente.class));

    List<Propuesta> resultado =
        gestor.generarPropuestas(necesidades, donaciones, DatosDeAsignacion.sinAsignaciones());

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

    when(algoritmo1Mock.ejecutar(anyList(), anyList(), any())).thenReturn(List.of(prop1));
    when(algoritmo2Mock.ejecutar(anyList(), anyList(), any())).thenReturn(List.of(prop2));

    List<Propuesta> resultado =
        gestor.generarPropuestas(List.of(), List.of(), DatosDeAsignacion.sinAsignaciones());

    assertEquals(2, resultado.size());
    assertTrue(resultado.contains(prop1));
    assertTrue(resultado.contains(prop2));
  }

  @Test
  void generarPropuestas_conListasVacias_retornaVacio() {
    when(algoritmo1Mock.ejecutar(anyList(), anyList(), any())).thenReturn(List.of());
    when(algoritmo2Mock.ejecutar(anyList(), anyList(), any())).thenReturn(List.of());

    List<Propuesta> resultado =
        gestor.generarPropuestas(List.of(), List.of(), DatosDeAsignacion.sinAsignaciones());

    assertTrue(resultado.isEmpty());
  }

  // A1 (spec R11-24): cada propuesta guarda el puesto en la lista de su algoritmo y de cuántas
  @Test
  void generarPropuestas_deUnSoloAlgoritmo_guardaPuestoYDeCuantas() {
    List<Propuesta> propuestas =
        List.of(paraNecesidad(), paraNecesidad(), paraNecesidad(), paraNecesidad());
    when(algoritmo1Mock.ejecutar(anyList(), anyList(), any())).thenReturn(propuestas);
    when(algoritmo2Mock.ejecutar(anyList(), anyList(), any())).thenReturn(List.of());

    List<Propuesta> resultado =
        gestor.generarPropuestas(List.of(), List.of(), DatosDeAsignacion.sinAsignaciones());

    assertEquals(4, resultado.size());
    for (int i = 0; i < 4; i++) {
      assertEquals(
          List.of(
              new SugerenciaDeAlgoritmo(TipoAlgoritmo.COMPATIBILIDAD_SEMANTICA, i + 1, 4, true)),
          resultado.get(i).getSugerencias());
    }
  }

  // A2: si los dos sugieren la misma necesidad, la propuesta que queda lleva las dos sugerencias
  @Test
  void generarPropuestas_cuandoLosDosSugierenLaMismaNecesidad_llevaLasDosSugerencias() {
    UUID necesidad = UUID.randomUUID();
    Propuesta deLaPrimera = new Propuesta();
    deLaPrimera.asociarNecesidad(necesidad);
    Propuesta deLaSegunda = new Propuesta();
    deLaSegunda.asociarNecesidad(necesidad);
    when(algoritmo1Mock.ejecutar(anyList(), anyList(), any()))
        .thenReturn(List.of(paraNecesidad(), paraNecesidad(), deLaPrimera));
    when(algoritmo2Mock.ejecutar(anyList(), anyList(), any()))
        .thenReturn(List.of(deLaSegunda, paraNecesidad()));

    List<Propuesta> resultado =
        gestor.generarPropuestas(List.of(), List.of(), DatosDeAsignacion.sinAsignaciones());

    assertEquals(List.of(deLaSegunda), resultado);
    assertEquals(
        List.of(
            new SugerenciaDeAlgoritmo(TipoAlgoritmo.PRIORIDAD_SUBATENDIDOS, 1, 2, true),
            new SugerenciaDeAlgoritmo(TipoAlgoritmo.COMPATIBILIDAD_SEMANTICA, 3, 3, false)),
        deLaSegunda.getSugerencias());
  }

  private static Propuesta paraNecesidad() {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(UUID.randomUUID());
    return propuesta;
  }
}
