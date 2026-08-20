package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.NecesidadResumenDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.repositories.IAsignacionesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.impl.AsignacionService;
import grupo5.donaciones.services.impl.LogisticaAsyncService;
import grupo5.donaciones.services.impl.PropuestaDeAsignacionService;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.PropuestaMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropuestaDeAsignacionServiceTest {

  private AsignacionService asignacionService;
  private IAsignacionesRepository asignacionRepository;
  private grupo5.donaciones.models.repositories.INecesidadesRepository necesidadRepository;
  private grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository
      donacionRepository;
  private PropuestaMapper propuestaMapper;
  private IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private IPersonasRepository personasRepository;
  private DireccionMapper direccionMapper;
  private LogisticaAsyncService logisticaAsyncService;
  private PropuestaDeAsignacionService propuestaDeAsignacionService;

  @BeforeEach
  void setUp() {
    asignacionService = mock(AsignacionService.class);
    asignacionRepository = mock(IAsignacionesRepository.class);
    necesidadRepository = mock(grupo5.donaciones.models.repositories.INecesidadesRepository.class);
    donacionRepository =
        mock(grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository.class);
    propuestaMapper = mock(PropuestaMapper.class);
    entidadesBeneficiariasRepository = mock(IEntidadesBeneficiariasRepository.class);
    personasRepository = mock(IPersonasRepository.class);
    direccionMapper = mock(DireccionMapper.class);
    logisticaAsyncService = mock(LogisticaAsyncService.class);

    propuestaDeAsignacionService =
        new PropuestaDeAsignacionService(
            asignacionService,
            asignacionRepository,
            necesidadRepository,
            donacionRepository,
            propuestaMapper,
            entidadesBeneficiariasRepository,
            personasRepository,
            direccionMapper,
            logisticaAsyncService);
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

    when(asignacionService.listarPropuestas()).thenReturn(List.of(propuesta));
    when(propuestaMapper.toDTO(propuesta)).thenReturn(dto);

    List<PropuestaDTO> resultado = propuestaDeAsignacionService.listarPropuestas();

    assertEquals(1, resultado.size());
    assertEquals(EstadoPropuesta.APROBADA, resultado.getFirst().estado());
    assertEquals("Alimentos", resultado.getFirst().necesidad().descripcion());

    verify(asignacionService).listarPropuestas();
    verify(propuestaMapper).toDTO(propuesta);
  }

  @Test
  void actualizarEstado_debeInvocarAlgoritmosService() {
    UUID id = UUID.randomUUID();
    EstadoPropuesta estado = EstadoPropuesta.APROBADA;

    propuestaDeAsignacionService.actualizarEstado(id, estado);

    verify(asignacionService).actualizarEstadoPropuesta(id, estado);
  }

  @Test
  void ejecutarAsignacion_debeInvocarAlgoritmosServiceYGuardarEjecucion() {
    Propuesta propuesta = mock(Propuesta.class);
    PropuestaDTO dto = mock(PropuestaDTO.class);
    when(asignacionService.generarPropuestas()).thenReturn(List.of(propuesta));
    when(propuestaMapper.toDTO(propuesta)).thenReturn(dto);

    List<PropuestaDTO> resultado = propuestaDeAsignacionService.ejecutarAsignacion();

    assertEquals(1, resultado.size());
    assertEquals(dto, resultado.getFirst());

    verify(asignacionService).generarPropuestas();
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

    List<EjecucionAsignacionDTO> resultado = propuestaDeAsignacionService.historialEjecuciones();

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

    propuestaDeAsignacionService.onPropuestaAprobada(event);

    verify(donacionOriginal).asignar("actor", necesidad);
    verify(necesidad).asignarDonacion(donacionOriginal);
    verify(donacionRepository, times(2)).save(donacionOriginal);
    verify(necesidadRepository).save(necesidad);
  }

  @Test
  void onPropuestaAprobada_debeNotificarALogistica_CuandoSeResuelvenTodosLosDatos() {
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
    UUID entidadId = UUID.randomUUID();
    UUID juridicaId = UUID.randomUUID();

    when(necesidadRepository.findById(necesidadId)).thenReturn(Optional.of(necesidad));
    when(donacionRepository.findById(donacionOriginalId)).thenReturn(Optional.of(donacionOriginal));
    when(fragmentacion.getDonacionOriginalId()).thenReturn(donacionOriginalId);
    when(fragmentacion.getCantidadNecesaria()).thenReturn(5);
    when(donacionOriginal.getCantidad()).thenReturn(5);
    when(donacionOriginal.getPesoTotal()).thenReturn(12.5);
    when(donacionOriginal.getVolumenTotal()).thenReturn(0.3);
    when(necesidad.getEntidadId()).thenReturn(entidadId);

    EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
    when(entidad.getId()).thenReturn(entidadId);
    when(entidad.juridicaId()).thenReturn(juridicaId);
    when(entidadesBeneficiariasRepository.findById(entidadId)).thenReturn(Optional.of(entidad));

    Juridica persona = mock(Juridica.class);
    Direccion direccionPersona = mock(Direccion.class);
    when(persona.getDireccion()).thenReturn(direccionPersona);
    when(personasRepository.findById(juridicaId)).thenReturn(Optional.of(persona));

    grupo5.donaciones.models.entities.propuestas.PropuestaAprobada event =
        new grupo5.donaciones.models.entities.propuestas.PropuestaAprobada(
            UUID.randomUUID(), necesidadId, List.of(fragmentacion), "actor");

    propuestaDeAsignacionService.onPropuestaAprobada(event);

    verify(direccionMapper).toOutputDTO(direccionPersona);
    verify(logisticaAsyncService)
        .registrarEntregaPendiente(
            argThat(
                request ->
                    request.idBeneficiaria().equals(entidadId)
                        && request.pesoTotalKG().equals(12.5)
                        && request.volumenTotalM3().equals(0.3)));
  }
}
