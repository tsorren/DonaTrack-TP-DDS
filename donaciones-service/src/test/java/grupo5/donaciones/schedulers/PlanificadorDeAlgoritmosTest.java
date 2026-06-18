package grupo5.donaciones.schedulers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.services.impl.PropuestaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlanificadorDeAlgoritmosTest {

  private PropuestaService propuestaServiceMock;
  private PlanificadorDeAlgoritmos planificador;

  @BeforeEach
  void setUp() {
    propuestaServiceMock = mock(PropuestaService.class);
    planificador = new PlanificadorDeAlgoritmos(propuestaServiceMock);
  }

  @Test
  void ejecutarAlgoritmos_deberiaInvocarPropuestaService() {
    planificador.ejecutarAlgoritmos();

    verify(propuestaServiceMock, times(1)).ejecutarAsignacion();
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
