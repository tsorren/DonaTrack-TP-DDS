package grupo5.donaciones.schedulers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import grupo5.donaciones.services.impl.PropuestaDeAsignacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlanificadorDeAlgoritmosTest {

  private PropuestaDeAsignacionService propuestaDeAsignacionServiceMock;
  private PlanificadorDeAlgoritmos planificador;

  @BeforeEach
  void setUp() {
    propuestaDeAsignacionServiceMock = mock(PropuestaDeAsignacionService.class);
    planificador = new PlanificadorDeAlgoritmos(propuestaDeAsignacionServiceMock);
  }

  @Test
  void ejecutarAlgoritmos_deberiaInvocarPropuestaService() {
    planificador.ejecutarAlgoritmos();

    verify(propuestaDeAsignacionServiceMock, times(1)).ejecutarAsignacion();
  }

  @Test
  void paraCuandoEstaPlanificado_deberiaRetornarDescripcionDeCron() {
    ReflectionTestUtils.setField(planificador, "cronExpression", "0 0 12 * * ?");

    String descripcion = planificador.paraCuandoEstaPlanificado();

    assertNotNull(descripcion);
    assertTrue(descripcion.contains("El scheduler está planificado para correr en:"));
    assertTrue(descripcion.contains("segundos: 0"));
    assertTrue(descripcion.contains("minutos: 0"));
    assertTrue(descripcion.contains("horas: 12"));
    assertTrue(descripcion.contains("día del mes: todos"));
    assertTrue(descripcion.contains("mes: todos"));
    assertTrue(descripcion.contains("día de la semana: cualquier"));
  }
}
