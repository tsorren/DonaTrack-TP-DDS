package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.NecesidadResumenDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IAsignacionesRepository;
import grupo5.donaciones.services.impl.AlgoritmosService;
import grupo5.donaciones.services.impl.PropuestaService;
import grupo5.donaciones.services.mappers.PropuestaMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropuestaServiceTest {

  private AlgoritmosService algoritmosService;
  private IAsignacionesRepository asignacionRepository;
  private grupo5.donaciones.models.repositories.INecesidadesRepository necesidadRepository;
  private grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository
      donacionRepository;
  private PropuestaMapper propuestaMapper;
  private PropuestaService propuestaService;

  @BeforeEach
  void setUp() {
    algoritmosService = mock(AlgoritmosService.class);
    asignacionRepository = mock(IAsignacionesRepository.class);
    necesidadRepository = mock(grupo5.donaciones.models.repositories.INecesidadesRepository.class);
    donacionRepository =
        mock(grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository.class);
    propuestaMapper = mock(PropuestaMapper.class);

    propuestaService =
        new PropuestaService(
            algoritmosService,
            asignacionRepository,
            necesidadRepository,
            donacionRepository,
            propuestaMapper);
  }

  @Test
  void listarPropuestas_debeRetornarLoDeAlgoritmosService() {
    Propuesta propuesta = mock(Propuesta.class);
    UUID id = UUID.randomUUID();
    NecesidadResumenDTO necesidadResumen =
        new NecesidadResumenDTO(UUID.randomUUID(), "Alimentos", 10, "EXTRAORDINARIA", "PENDIENTE");
    PropuestaDTO dto =
        new PropuestaDTO(
            id,
            EstadoPropuesta.APROBADA,
            java.time.LocalDateTime.now(),
            necesidadResumen,
            List.of());

    when(algoritmosService.listarPropuestas()).thenReturn(List.of(propuesta));
    when(propuestaMapper.toDTO(propuesta)).thenReturn(dto);

    List<PropuestaDTO> resultado = propuestaService.listarPropuestas();

    assertEquals(1, resultado.size());
    assertEquals(EstadoPropuesta.APROBADA, resultado.getFirst().estado());
    assertEquals("Alimentos", resultado.getFirst().necesidad().descripcion());

    verify(algoritmosService).listarPropuestas();
    verify(propuestaMapper).toDTO(propuesta);
  }

  @Test
  void actualizarEstado_debeInvocarAlgoritmosService() {
    UUID id = UUID.randomUUID();
    EstadoPropuesta estado = EstadoPropuesta.APROBADA;

    propuestaService.actualizarEstado(id, estado);

    verify(algoritmosService).actualizarEstadoPropuesta(id, estado);
  }

  @Test
  void ejecutarAsignacion_debeInvocarAlgoritmosServiceYGuardarEjecucion() {
    Propuesta propuesta = mock(Propuesta.class);
    PropuestaDTO dto = mock(PropuestaDTO.class);
    when(algoritmosService.ejecutar()).thenReturn(List.of(propuesta));
    when(propuestaMapper.toDTO(propuesta)).thenReturn(dto);

    List<PropuestaDTO> resultado = propuestaService.ejecutarAsignacion();

    assertEquals(1, resultado.size());
    assertEquals(dto, resultado.getFirst());

    verify(algoritmosService).ejecutar();
    verify(propuestaMapper).toDTO(propuesta);
    verify(asignacionRepository).save(any(EjecucionAsignacionDTO.class));
  }

  // Helper type matching for any EjecucionAsignacionDTO - kept for reference
  @SuppressWarnings("unused")
  private static class RunEjecucionAsignacionMockGuardar extends EjecucionAsignacionDTO {}

  @Test
  void historialEjecuciones_debeRetornarLoDelRepositorio() {
    EjecucionAsignacionDTO dto = mock(EjecucionAsignacionDTO.class);
    when(asignacionRepository.obtenerHistorial()).thenReturn(List.of(dto));

    List<EjecucionAsignacionDTO> resultado = propuestaService.historialEjecuciones();

    assertEquals(1, resultado.size());
    assertEquals(dto, resultado.getFirst());

    verify(asignacionRepository).obtenerHistorial();
  }

  @Test
  void onPropuestaAprobada_debeMutarYPersistir() {
    Necesidad necesidad = mock(Necesidad.class);
    grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion fragmentacion =
        mock(grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion.class);
    grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente
        donacionOriginal =
            mock(
                grupo5
                    .donaciones
                    .models
                    .entities
                    .donacionesIndependientes
                    .DonacionIndependiente
                    .class);

    UUID necesidadId = UUID.randomUUID();
    UUID donacionOriginalId = UUID.randomUUID();
    when(necesidadRepository.findById(necesidadId)).thenReturn(java.util.Optional.of(necesidad));
    when(donacionRepository.findById(donacionOriginalId))
        .thenReturn(java.util.Optional.of(donacionOriginal));

    when(fragmentacion.getDonacionOriginalId()).thenReturn(donacionOriginalId);
    when(fragmentacion.getCantidadNecesaria()).thenReturn(5);
    when(donacionOriginal.getCantidad()).thenReturn(5); // Exact quantity, no fragmentation

    grupo5.donaciones.models.entities.propuestas.PropuestaAprobada event =
        new grupo5.donaciones.models.entities.propuestas.PropuestaAprobada(
            UUID.randomUUID(), necesidadId, List.of(fragmentacion), "actor");

    propuestaService.onPropuestaAprobada(event);

    verify(donacionOriginal).asignar("actor", necesidad);
    verify(necesidad).asignarDonacion(donacionOriginal);
    verify(donacionRepository, times(2)).save(donacionOriginal);
    verify(necesidadRepository).save(necesidad);
  }
}
