package grupo5.logistica.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.services.impl.AsignadorDeEntregasPorDimension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlgoritmoAsignadorDeEntregaTest {

  private AsignadorDeEntregasPorDimension asignador;

  @BeforeEach
  void setUp() {
    asignador = new AsignadorDeEntregasPorDimension();
  }

  // ===================== asignar() =====================

  @Test
  void asignar_deberiaAsignarEntregaAlPrimerCamionConCapacidad() {

    Camion camion1 = mock(Camion.class);
    Camion camion2 = mock(Camion.class);

    when(camion1.getCapacidadKG()).thenReturn(100f);
    when(camion1.getCapacidadVolumen()).thenReturn(10f);

    when(camion2.getCapacidadKG()).thenReturn(200f);
    when(camion2.getCapacidadVolumen()).thenReturn(20f);

    Entrega entrega = mock(Entrega.class);

    when(entrega.getPesoTotalKG()).thenReturn(50f);
    when(entrega.getVolumenTotalM3()).thenReturn(5f);

    Map<Camion, List<Entrega>> resultado =
        asignador.asignar(List.of(entrega), List.of(camion1, camion2));

    assertEquals(1, resultado.size());
    assertTrue(resultado.containsKey(camion1));
    assertEquals(List.of(entrega), resultado.get(camion1));
  }

  @Test
  void asignar_deberiaUsarSegundoCamionCuandoPrimeroNoTieneCapacidad() {

    Camion camion1 = mock(Camion.class);
    Camion camion2 = mock(Camion.class);

    when(camion1.getCapacidadKG()).thenReturn(20f);
    when(camion1.getCapacidadVolumen()).thenReturn(2f);

    when(camion2.getCapacidadKG()).thenReturn(100f);
    when(camion2.getCapacidadVolumen()).thenReturn(10f);

    Entrega entrega = mock(Entrega.class);

    when(entrega.getPesoTotalKG()).thenReturn(50f);
    when(entrega.getVolumenTotalM3()).thenReturn(5f);

    Map<Camion, List<Entrega>> resultado =
        asignador.asignar(List.of(entrega), List.of(camion1, camion2));

    assertEquals(1, resultado.size());
    assertFalse(resultado.containsKey(camion1));
    assertTrue(resultado.containsKey(camion2));
  }

  @Test
  void asignar_deberiaDejarEntregaSinAsignarCuandoNingunCamionTieneCapacidad() {

    Camion camion = mock(Camion.class);

    when(camion.getCapacidadKG()).thenReturn(10f);
    when(camion.getCapacidadVolumen()).thenReturn(1f);

    Entrega entrega = mock(Entrega.class);

    when(entrega.getPesoTotalKG()).thenReturn(50f);
    when(entrega.getVolumenTotalM3()).thenReturn(5f);

    Map<Camion, List<Entrega>> resultado = asignador.asignar(List.of(entrega), List.of(camion));

    assertTrue(resultado.isEmpty());
  }

  @Test
  void asignar_deberiaAcumularVariasEntregasMientrasHayaCapacidad() {

    Camion camion = mock(Camion.class);

    when(camion.getCapacidadKG()).thenReturn(100f);
    when(camion.getCapacidadVolumen()).thenReturn(10f);

    Entrega entrega1 = mock(Entrega.class);
    Entrega entrega2 = mock(Entrega.class);

    when(entrega1.getPesoTotalKG()).thenReturn(30f);
    when(entrega1.getVolumenTotalM3()).thenReturn(3f);

    when(entrega2.getPesoTotalKG()).thenReturn(40f);
    when(entrega2.getVolumenTotalM3()).thenReturn(4f);

    Map<Camion, List<Entrega>> resultado =
        asignador.asignar(List.of(entrega1, entrega2), List.of(camion));

    assertEquals(1, resultado.size());
    assertEquals(2, resultado.get(camion).size());
  }

  @Test
  void asignar_deberiaLanzarExcepcionCuandoEntregasEsNull() {

    Camion camion = mock(Camion.class);

    assertThrows(ValidationException.class, () -> asignador.asignar(null, List.of(camion)));
  }

  @Test
  void asignar_deberiaLanzarExcepcionCuandoCamionesEsNull() {

    Entrega entrega = mock(Entrega.class);

    assertThrows(ValidationException.class, () -> asignador.asignar(List.of(entrega), null));
  }
}
