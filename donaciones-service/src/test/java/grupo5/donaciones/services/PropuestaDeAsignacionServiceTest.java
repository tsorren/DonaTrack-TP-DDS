package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
<<<<<<< HEAD
import static org.mockito.Mockito.*;

=======
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.RecursoNoEncontradoException;
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.NecesidadResumenDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
<<<<<<< HEAD
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
=======
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.propuestas.EjecucionAsignacion;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.GestorPropuestasDeAsignacion;
import grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.entities.propuestas.PropuestaAprobada;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.repositories.IAsignacionesRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.services.impl.LogisticaAsyncService;
import grupo5.donaciones.services.impl.PropuestaDeAsignacionService;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.EjecucionAsignacionMapper;
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
import grupo5.donaciones.services.mappers.PropuestaMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
<<<<<<< HEAD

class PropuestaDeAsignacionServiceTest {

  private AsignacionService asignacionService;
  private IAsignacionesRepository asignacionRepository;
  private grupo5.donaciones.models.repositories.INecesidadesRepository necesidadRepository;
  private grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository
      donacionRepository;
  private PropuestaMapper propuestaMapper;
=======
import org.springframework.context.ApplicationEventPublisher;

class PropuestaDeAsignacionServiceTest {

  private GestorPropuestasDeAsignacion gestorPropuestas;
  private IDonacionesIndependientesRepository donacionRepository;
  private INecesidadesRepository necesidadRepository;
  private IPropuestasRepository propuestaRepository;
  private IAsignacionesRepository asignacionRepository;
  private PropuestaMapper propuestaMapper;
  private EjecucionAsignacionMapper ejecucionMapper;
  private ApplicationEventPublisher eventPublisher;
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
  private IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private IPersonasRepository personasRepository;
  private DireccionMapper direccionMapper;
  private LogisticaAsyncService logisticaAsyncService;
<<<<<<< HEAD
  private PropuestaDeAsignacionService propuestaDeAsignacionService;

  @BeforeEach
  void setUp() {
    asignacionService = mock(AsignacionService.class);
    asignacionRepository = mock(IAsignacionesRepository.class);
    necesidadRepository = mock(grupo5.donaciones.models.repositories.INecesidadesRepository.class);
    donacionRepository =
        mock(grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository.class);
    propuestaMapper = mock(PropuestaMapper.class);
=======
  private PropuestaDeAsignacionService service;

  @BeforeEach
  void setUp() {
    gestorPropuestas = mock(GestorPropuestasDeAsignacion.class);
    donacionRepository = mock(IDonacionesIndependientesRepository.class);
    necesidadRepository = mock(INecesidadesRepository.class);
    propuestaRepository = mock(IPropuestasRepository.class);
    asignacionRepository = mock(IAsignacionesRepository.class);
    propuestaMapper = mock(PropuestaMapper.class);
    ejecucionMapper = mock(EjecucionAsignacionMapper.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
    entidadesBeneficiariasRepository = mock(IEntidadesBeneficiariasRepository.class);
    personasRepository = mock(IPersonasRepository.class);
    direccionMapper = mock(DireccionMapper.class);
    logisticaAsyncService = mock(LogisticaAsyncService.class);

<<<<<<< HEAD
    propuestaDeAsignacionService =
        new PropuestaDeAsignacionService(
            asignacionService,
            asignacionRepository,
            necesidadRepository,
            donacionRepository,
            propuestaMapper,
=======
    service =
        new PropuestaDeAsignacionService(
            gestorPropuestas,
            donacionRepository,
            necesidadRepository,
            propuestaRepository,
            asignacionRepository,
            propuestaMapper,
            ejecucionMapper,
            eventPublisher,
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
            entidadesBeneficiariasRepository,
            personasRepository,
            direccionMapper,
            logisticaAsyncService);
  }

  @Test
<<<<<<< HEAD
  void listarPropuestas_debeRetornarLoDeAlgoritmosService() {
=======
  void ejecutarAsignacion_debeInvocarGestorYGuardarEjecucion() {
    DonacionIndependiente donacion = mock(DonacionIndependiente.class);
    Necesidad necesidad = mock(Necesidad.class);
    Propuesta propuesta = mock(Propuesta.class);
    PropuestaDTO dto = mock(PropuestaDTO.class);

    when(donacionRepository.findEnDeposito()).thenReturn(List.of(donacion));
    when(necesidadRepository.findByEstaSatisfechaFalseActivaTrue()).thenReturn(List.of(necesidad));
    when(gestorPropuestas.generarPropuestas(List.of(necesidad), List.of(donacion)))
        .thenReturn(List.of(propuesta));
    when(propuestaMapper.toDTO(propuesta)).thenReturn(dto);

    List<PropuestaDTO> resultado = service.ejecutarAsignacion();

    assertEquals(1, resultado.size());
    assertEquals(dto, resultado.getFirst());

    verify(propuestaRepository).save(propuesta);
    verify(asignacionRepository).save(any(EjecucionAsignacion.class));
  }

  @Test
  void listarPropuestas_debeRetornarLoDelRepositorio() {
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
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

<<<<<<< HEAD
    when(asignacionService.listarPropuestas()).thenReturn(List.of(propuesta));
    when(propuestaMapper.toDTO(propuesta)).thenReturn(dto);

    List<PropuestaDTO> resultado = propuestaDeAsignacionService.listarPropuestas();

    assertEquals(1, resultado.size());
    assertEquals(EstadoPropuesta.APROBADA, resultado.getFirst().estado());
    assertEquals("Alimentos", resultado.getFirst().necesidad().descripcion());

    verify(asignacionService).listarPropuestas();
=======
    when(propuestaRepository.findAll()).thenReturn(List.of(propuesta));
    when(propuestaMapper.toDTO(propuesta)).thenReturn(dto);

    List<PropuestaDTO> resultado = service.listarPropuestas();

    assertEquals(1, resultado.size());
    assertEquals(EstadoPropuesta.APROBADA, resultado.getFirst().estado());
    verify(propuestaRepository).findAll();
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
    verify(propuestaMapper).toDTO(propuesta);
  }

  @Test
<<<<<<< HEAD
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
=======
  void actualizarEstado_cuandoEsAprobada_debeAprobarYPublicarEvento() {
    UUID id = UUID.randomUUID();
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(UUID.randomUUID());

    when(propuestaRepository.findById(id)).thenReturn(Optional.of(propuesta));

    service.actualizarEstado(id, EstadoPropuesta.APROBADA);

    assertEquals(EstadoPropuesta.APROBADA, propuesta.getEstado());
    verify(eventPublisher, times(1)).publishEvent(any(PropuestaAprobada.class));
    verify(propuestaRepository).save(propuesta);
  }

  @Test
  void actualizarEstado_cuandoEsDescartada_debeRechazarPropuesta() {
    UUID id = UUID.randomUUID();
    Propuesta propuesta = new Propuesta();
    when(propuestaRepository.findById(id)).thenReturn(Optional.of(propuesta));

    service.actualizarEstado(id, EstadoPropuesta.DESCARTADA);

    assertEquals(EstadoPropuesta.DESCARTADA, propuesta.getEstado());
    verify(propuestaRepository).save(propuesta);
  }

  @Test
  void actualizarEstado_cuandoNoExiste_debeLanzarRecursoNoEncontrado() {
    UUID id = UUID.randomUUID();
    when(propuestaRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNoEncontradoException.class,
        () -> service.actualizarEstado(id, EstadoPropuesta.APROBADA));
  }

  @Test
  void historialEjecuciones_debeRetornarLoDelRepositorioMapeado() {
    EjecucionAsignacion entity = mock(EjecucionAsignacion.class);
    EjecucionAsignacionDTO dto = mock(EjecucionAsignacionDTO.class);
    when(asignacionRepository.obtenerHistorial()).thenReturn(List.of(entity));
    when(ejecucionMapper.toDTO(entity)).thenReturn(dto);

    List<EjecucionAsignacionDTO> resultado = service.historialEjecuciones();

    assertEquals(1, resultado.size());
    assertEquals(dto, resultado.getFirst());
    verify(asignacionRepository).obtenerHistorial();
    verify(ejecucionMapper).toDTO(entity);
  }

  @Test
  void onPropuestaAprobada_debeInvocarConfirmarEnFragmentacionYPersistir() {
    Necesidad necesidad = mock(Necesidad.class);
    PosibleFragmentacion fragmentacion = mock(PosibleFragmentacion.class);
    DonacionIndependiente donacionOriginal = mock(DonacionIndependiente.class);
    DonacionIndependiente donacionAsignada = mock(DonacionIndependiente.class);

    UUID necesidadId = UUID.randomUUID();
    UUID donacionOriginalId = UUID.randomUUID();

    when(necesidadRepository.findById(necesidadId)).thenReturn(Optional.of(necesidad));
    when(donacionRepository.findById(donacionOriginalId)).thenReturn(Optional.of(donacionOriginal));
    when(fragmentacion.getDonacionOriginalId()).thenReturn(donacionOriginalId);
    when(fragmentacion.confirmar(necesidad, "actor")).thenReturn(donacionAsignada);

    PropuestaAprobada event =
        new PropuestaAprobada(UUID.randomUUID(), necesidadId, List.of(fragmentacion), "actor");

    service.onPropuestaAprobada(event);

    verify(fragmentacion).setDonacionOriginal(donacionOriginal);
    verify(fragmentacion).confirmar(necesidad, "actor");
    verify(donacionRepository).save(donacionOriginal);
    verify(donacionRepository).save(donacionAsignada);
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
    verify(necesidadRepository).save(necesidad);
  }

  @Test
  void onPropuestaAprobada_debeNotificarALogistica_CuandoSeResuelvenTodosLosDatos() {
    Necesidad necesidad = mock(Necesidad.class);
<<<<<<< HEAD
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
=======
    PosibleFragmentacion fragmentacion = mock(PosibleFragmentacion.class);
    DonacionIndependiente donacionOriginal = mock(DonacionIndependiente.class);
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb

    UUID necesidadId = UUID.randomUUID();
    UUID donacionOriginalId = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();
    UUID juridicaId = UUID.randomUUID();

    when(necesidadRepository.findById(necesidadId)).thenReturn(Optional.of(necesidad));
    when(donacionRepository.findById(donacionOriginalId)).thenReturn(Optional.of(donacionOriginal));
    when(fragmentacion.getDonacionOriginalId()).thenReturn(donacionOriginalId);
<<<<<<< HEAD
    when(fragmentacion.getCantidadNecesaria()).thenReturn(5);
    when(donacionOriginal.getCantidad()).thenReturn(5);
=======
    when(fragmentacion.confirmar(necesidad, "actor")).thenReturn(donacionOriginal);
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
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

<<<<<<< HEAD
    grupo5.donaciones.models.entities.propuestas.PropuestaAprobada event =
        new grupo5.donaciones.models.entities.propuestas.PropuestaAprobada(
            UUID.randomUUID(), necesidadId, List.of(fragmentacion), "actor");

    propuestaDeAsignacionService.onPropuestaAprobada(event);
=======
    PropuestaAprobada event =
        new PropuestaAprobada(UUID.randomUUID(), necesidadId, List.of(fragmentacion), "actor");

    service.onPropuestaAprobada(event);
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb

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
