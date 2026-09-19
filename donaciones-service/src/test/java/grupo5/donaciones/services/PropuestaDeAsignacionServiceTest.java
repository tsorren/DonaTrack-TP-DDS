package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionAsignadaV1;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.NecesidadResumenDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
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
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.services.impl.PropuestaDeAsignacionService;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.EjecucionAsignacionMapper;
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
  private EjecucionAsignacionMapper ejecucionMapper;
  private ApplicationEventPublisher eventPublisher;
  private IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private IPersonasRepository personasRepository;
  private DireccionMapper direccionMapper;
  private IDonacionesRepository donacionesRepository;
  private IDonantesRepository donantesRepository;
  private IDonacionesEventPublisher donacionesEventPublisher;
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
    entidadesBeneficiariasRepository = mock(IEntidadesBeneficiariasRepository.class);
    personasRepository = mock(IPersonasRepository.class);
    direccionMapper = mock(DireccionMapper.class);
    donacionesRepository = mock(IDonacionesRepository.class);
    donantesRepository = mock(IDonantesRepository.class);
    donacionesEventPublisher = mock(IDonacionesEventPublisher.class);

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
            entidadesBeneficiariasRepository,
            personasRepository,
            direccionMapper,
            donacionesRepository,
            donantesRepository,
            donacionesEventPublisher);
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
    verify(asignacionRepository).save(any(EjecucionAsignacion.class));
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
    verify(necesidadRepository).save(necesidad);
  }

  @Test
  void onPropuestaAprobada_debePublicarDonacionAsignada_CuandoSeResuelvenTodosLosDatos() {
    Necesidad necesidad = mock(Necesidad.class);
    PosibleFragmentacion fragmentacion = mock(PosibleFragmentacion.class);
    DonacionIndependiente donacionOriginal = mock(DonacionIndependiente.class);

    UUID necesidadId = UUID.randomUUID();
    UUID donacionOriginalId = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();
    UUID juridicaId = UUID.randomUUID();
    UUID donanteId = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();

    when(necesidadRepository.findById(necesidadId)).thenReturn(Optional.of(necesidad));
    when(donacionRepository.findById(donacionOriginalId)).thenReturn(Optional.of(donacionOriginal));
    when(fragmentacion.getDonacionOriginalId()).thenReturn(donacionOriginalId);
    when(fragmentacion.confirmar(necesidad, "actor")).thenReturn(donacionOriginal);
    when(donacionOriginal.getDonacionOriginalId()).thenReturn(donacionOriginalId);
    when(donacionOriginal.getPesoTotal()).thenReturn(12.5);
    when(donacionOriginal.getVolumenTotal()).thenReturn(0.3);
    when(necesidad.getEntidadId()).thenReturn(entidadId);

    EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
    when(entidad.juridicaId()).thenReturn(juridicaId);
    when(entidadesBeneficiariasRepository.findById(entidadId)).thenReturn(Optional.of(entidad));

    Juridica persona = mock(Juridica.class);
    Direccion direccionPersona = mock(Direccion.class);
    when(persona.getDireccion()).thenReturn(direccionPersona);
    when(personasRepository.findById(juridicaId)).thenReturn(Optional.of(persona));

    Donacion donacion = mock(Donacion.class);
    when(donacion.getDonanteId()).thenReturn(donanteId);
    when(donacionesRepository.findById(donacionOriginalId)).thenReturn(Optional.of(donacion));

    Donante donante = mock(Donante.class);
    when(donante.personaId()).thenReturn(personaId);
    when(donantesRepository.findById(donanteId)).thenReturn(Optional.of(donante));

    PropuestaAprobada event =
        new PropuestaAprobada(UUID.randomUUID(), necesidadId, List.of(fragmentacion), "actor");

    service.onPropuestaAprobada(event);

    verify(direccionMapper).toDestinoEventoDTO(direccionPersona);
    verify(donacionesEventPublisher)
        .publicarDonacionAsignada(
            argThat(
                (EventoDonacionAsignadaV1 evento) ->
                    evento.personaBeneficiariaId().equals(juridicaId)
                        && evento.donanteId().equals(donanteId)
                        && evento.personaId().equals(personaId)
                        && evento.pesoTotalKG().equals(12.5)
                        && evento.volumenTotalM3().equals(0.3)));
  }
}
