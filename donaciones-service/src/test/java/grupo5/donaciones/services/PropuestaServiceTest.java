package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.propuestas.PropuestaResponseDTO;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.IAsignacionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PropuestaServiceTest {

  private AlgoritmosService algoritmosService;
  private PropuestaService propuestaService;

  @BeforeEach
  void setUp() {
    algoritmosService = mock(AlgoritmosService.class);
    IAsignacionRepository asignacionRepository = mock(IAsignacionRepository.class);

    propuestaService = new PropuestaService(algoritmosService, asignacionRepository);
  }

  @Test
  void listarPropuestas_debeRetornarLoDeAlgoritmosService() {
    Propuesta propuesta = mock(Propuesta.class);
    Necesidad necesidad = mock(Necesidad.class);

    when(propuesta.getId()).thenReturn(UUID.randomUUID());
    when(propuesta.getEstado()).thenReturn(EstadoPropuesta.APROBADA);
    when(propuesta.getNecesidadQueSatisface()).thenReturn(necesidad);
    when(necesidad.getDescripcion()).thenReturn("Alimentos");

    when(algoritmosService.listarPropuestas()).thenReturn(List.of(propuesta));

    List<PropuestaResponseDTO> resultado = propuestaService.listarPropuestas();

    assertEquals(1, resultado.size());
    assertEquals("APROBADA", resultado.getFirst().estado());

    verify(algoritmosService).listarPropuestas();
  }

  @Test
  void actualizarEstado_debeInvocarAlgoritmosService() {
    UUID id = UUID.randomUUID();
    EstadoPropuesta estado = EstadoPropuesta.APROBADA;

    propuestaService.actualizarEstado(id, estado);

    verify(algoritmosService).actualizarEstadoPropuesta(id, estado);
  }
}
