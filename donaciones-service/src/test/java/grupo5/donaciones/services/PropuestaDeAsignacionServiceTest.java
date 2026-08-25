package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.NecesidadResumenDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Juridica;
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
import grupo5.donaciones.services.mappers.PropuestaMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class PropuestaDeAsignacionServiceTest {

  private GestorPropuestasDeAsignacion gestorPropuestas;
  private IDonacionesIndependientesRepository donacionRepository;
  private INecesidadesRepository necesidadRepository;
  private IPropuestasRepository propuestaRepository;
  private IAsignacionesRepository asignacionRepository;
  private PropuestaMapper propuestaMapper;
  private ApplicationEventPublisher eventPublisher;
  private IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private IPersonasRepository personasRepository;
  private DireccionMapper direccionMapper;
  private LogisticaAsyncService logisticaAsyncService;
  private PropuestaDeAsignacionService service;

  @BeforeEach
  void setUp() {
    gestorPropuestas = mock(GestorPropuestasDeAsignacion.class);
    donacionRepository = mock(IDonacionesIndependientesRepository.class);
    necesidadRepository = mock(INecesidadesRepository.class);
    propuestaRepository = mock(IPropuestasRepository.class);
    asignacionRepository = mock(IAsignacionesRepository.class);
    propuestaMapper = mock(PropuestaMapper.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    entidadesBeneficiariasRepository = mock(IEntidadesBeneficiariasRepository.class);
    personasRepository = mock(IPersonasRepository.class);
    direccionMapper = mock(DireccionMapper.class);
    logisticaAsyncService = mock(LogisticaAsyncService.class);

    service =
        new PropuestaDeAsignacionService(
            gestorPropuestas,
            donacionRepository,
            necesidadRepository,
            propuestaRepository,
            asignacionRepository,
            propuestaMapper,
            eventPublisher,
            entidadesBeneficiariasRepository,
            personasRepository,
            direccionMapper,
            logisticaAsyncService);
  }

  @Test
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
    verify(asignacionRepository).save(any(EjecucionAsignacionDTO.class));
  }

  @Test
  void listarPropuestas_debeRetornarLoDelRepositorio() {
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

    when(propuestaRepository.findAll()).thenReturn(List.of(propuesta));
    when(propuestaMapper.toDTO(propuesta)).thenReturn(dto);

    List<PropuestaDTO> resultado = service.listarPropuestas();

    assertEquals(1, resultado.size());
    assertEquals(EstadoPropuesta.APROBADA, resultado.getFirst().estado());
    verify(propuestaRepository).findAll();
    verify(propuestaMapper).toDTO(propuesta);
  }

  @Test
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
  void historialEjecuciones_debeRetornarLoDelRepositorio() {
    EjecucionAsignacionDTO dto = mock(EjecucionAsignacionDTO.class);
    when(asignacionRepository.obtenerHistorial()).thenReturn(List.of(dto));

    List<EjecucionAsignacionDTO> resultado = service.historialEjecuciones();

    assertEquals(1, resultado.size());
    assertEquals(dto, resultado.getFirst());
    verify(asignacionRepository).obtenerHistorial();
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
    verify(necesidadRepository).save(necesidad);
  }

  @Test
  void onPropuestaAprobada_debeNotificarALogistica_CuandoSeResuelvenTodosLosDatos() {
    Necesidad necesidad = mock(Necesidad.class);
    PosibleFragmentacion fragmentacion = mock(PosibleFragmentacion.class);
    DonacionIndependiente donacionOriginal = mock(DonacionIndependiente.class);

    UUID necesidadId = UUID.randomUUID();
    UUID donacionOriginalId = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();
    UUID juridicaId = UUID.randomUUID();

    when(necesidadRepository.findById(necesidadId)).thenReturn(Optional.of(necesidad));
    when(donacionRepository.findById(donacionOriginalId)).thenReturn(Optional.of(donacionOriginal));
    when(fragmentacion.getDonacionOriginalId()).thenReturn(donacionOriginalId);
    when(fragmentacion.confirmar(necesidad, "actor")).thenReturn(donacionOriginal);
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

    PropuestaAprobada event =
        new PropuestaAprobada(UUID.randomUUID(), necesidadId, List.of(fragmentacion), "actor");

    service.onPropuestaAprobada(event);

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
