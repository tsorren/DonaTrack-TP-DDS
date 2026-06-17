package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IAsignacionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class AsignacionServiceTest {
  private IAsignacionRepository asignacionRepository;
  private AlgoritmosService algoritmosService;
  private AsignacionService asignacionService;

  @BeforeEach
  void setUp() {
    asignacionRepository = mock(IAsignacionRepository.class);
    algoritmosService = mock(AlgoritmosService.class);

    asignacionService = new AsignacionService(asignacionRepository, algoritmosService);
  }

  @Test
  void ejecutarAsignacion_debeRegistrarEjecucionEnHistorial() {

    List<Propuesta> propuestas = List.of(new Propuesta(), new Propuesta());

    when(algoritmosService.ejecutar()).thenReturn(propuestas);

    asignacionService.ejecutarAsignacion();

    verify(asignacionRepository).save(any(EjecucionAsignacionDTO.class));
  }

  @Test
  void ejecutarAsignacion_debeGuardarCantidadDePropuestasGeneradas() {

    List<Propuesta> propuestas = List.of(new Propuesta(), new Propuesta(), new Propuesta());

    when(algoritmosService.ejecutar()).thenReturn(propuestas);

    asignacionService.ejecutarAsignacion();

    ArgumentCaptor<EjecucionAsignacionDTO> captor =
        ArgumentCaptor.forClass(EjecucionAsignacionDTO.class);

    verify(asignacionRepository).save(captor.capture());

    assertEquals(3, captor.getValue().getCantidadPropuestasGeneradas());
  }

  @Test
  void historial_debeRetornarLoDelRepositorio() {

    List<EjecucionAsignacionDTO> historial = List.of(new EjecucionAsignacionDTO());

    when(asignacionRepository.obtenerHistorial()).thenReturn(historial);

    List<EjecucionAsignacionDTO> resultado = asignacionService.historial();

    assertEquals(historial, resultado);
  }

  @Test
  void ejecutarAsignacion_debeRetornarLasPropuestasGeneradas() {

    List<Propuesta> propuestas = List.of(new Propuesta(), new Propuesta());

    when(algoritmosService.ejecutar()).thenReturn(propuestas);

    List<Propuesta> resultado = asignacionService.ejecutarAsignacion();

    assertEquals(propuestas, resultado);
  }

  @Test
  void ejecutarAsignacion_debeInvocarAlgoritmosService() {

    when(algoritmosService.ejecutar()).thenReturn(List.of());

    asignacionService.ejecutarAsignacion();

    verify(algoritmosService).ejecutar();
  }
}
