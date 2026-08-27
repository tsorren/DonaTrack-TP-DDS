package grupo5.logistica.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.planificacion.AlgoritmoOrdenadorSimple;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlgoritmoOrdenarEntregasTest {

  private AlgoritmoOrdenadorSimple algoritmo;

  @BeforeEach
  void setUp() {
    algoritmo = new AlgoritmoOrdenadorSimple();
  }

  // ===================== obtenerEntregasOrdenadas() =====================

  @Test
  void obtenerEntregasOrdenadas_deberiaOrdenarPorIdAscendente() {

    Entrega entrega1 = mock(Entrega.class);
    Entrega entrega2 = mock(Entrega.class);
    Entrega entrega3 = mock(Entrega.class);

    UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    UUID id3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    when(entrega1.getId()).thenReturn(id3);
    when(entrega2.getId()).thenReturn(id1);
    when(entrega3.getId()).thenReturn(id2);

    List<Entrega> resultado =
        algoritmo.obtenerEntregasOrdenadas(List.of(entrega1, entrega2, entrega3));

    assertEquals(3, resultado.size());
    assertEquals(id1, resultado.get(0).getId());
    assertEquals(id2, resultado.get(1).getId());
    assertEquals(id3, resultado.get(2).getId());
  }

  @Test
  void obtenerEntregasOrdenadas_deberiaRetornarListaVacia_cuandoNoHayEntregas() {

    List<Entrega> resultado = algoritmo.obtenerEntregasOrdenadas(List.of());

    assertTrue(resultado.isEmpty());
  }

  @Test
  void obtenerEntregasOrdenadas_deberiaLanzarExcepcion_cuandoListaEsNull() {

    assertThrows(ValidationException.class, () -> algoritmo.obtenerEntregasOrdenadas(null));
  }
}
