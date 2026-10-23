package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.DestinoEventoDTO;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionAsignadaV1;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
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
import grupo5.donaciones.services.impl.AcumuladosDeAsignacion;
import grupo5.donaciones.services.impl.PropuestaDeAsignacionService;
import grupo5.donaciones.services.logistica.ILogisticaBroker;
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
  private ILogisticaBroker logisticaBroker;
  private AcumuladosDeAsignacion acumulados;
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
    logisticaBroker = mock(ILogisticaBroker.class);
    acumulados = mock(AcumuladosDeAsignacion.class);

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
            donacionesEventPublisher,
            logisticaBroker,
            acumulados);
  }

  @Test
  void ejecutarAsignacion_debeInvocarGestorYGuardarEjecucion() {
    DonacionIndependiente donacion = mock(DonacionIndependiente.class);
    Necesidad necesidad = mock(Necesidad.class);
    Propuesta propuesta = mock(Propuesta.class);
    PropuestaDTO dto = mock(PropuestaDTO.class);

    when(donacionRepository.findEnDeposito()).thenReturn(List.of(donacion));
    Necesidad satisfecha = mock(Necesidad.class);
    when(necesidadRepository.findActivas()).thenReturn(List.of(necesidad, satisfecha));
    when(acumulados.estaSatisfecha(satisfecha)).thenReturn(true);
    when(gestorPropuestas.generarPropuestas(eq(List.of(necesidad)), eq(List.of(donacion)), any()))
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
    Necesidad necesidad = necesidadActivaDe(UUID.randomUUID());
    propuesta.asociarNecesidad(necesidad.getId());

    when(propuestaRepository.findById(id)).thenReturn(Optional.of(propuesta));
    when(necesidadRepository.findById(necesidad.getId())).thenReturn(Optional.of(necesidad));

    service.actualizarEstado(id, EstadoPropuesta.APROBADA);

    assertEquals(EstadoPropuesta.APROBADA, propuesta.getEstado());
    verify(eventPublisher, times(1)).publishEvent(any(PropuestaAprobada.class));
    verify(propuestaRepository).save(propuesta);
  }

  @Test
  void actualizarEstado_aprobadaConNecesidadInactiva_lanzaNecesidadInactivaYNoPublica() {
    UUID id = UUID.randomUUID();
    Propuesta propuesta = new Propuesta();
    Necesidad necesidad = mock(Necesidad.class);
    when(necesidad.getId()).thenReturn(UUID.randomUUID());
    when(necesidad.isActiva()).thenReturn(false);
    propuesta.asociarNecesidad(necesidad.getId());
    when(propuestaRepository.findById(id)).thenReturn(Optional.of(propuesta));
    when(necesidadRepository.findById(necesidad.getId())).thenReturn(Optional.of(necesidad));

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> service.actualizarEstado(id, EstadoPropuesta.APROBADA));

    assertEquals(ErrorCatalog.NECESIDAD_INACTIVA, ex.getError());
    assertEquals(EstadoPropuesta.PENDIENTE, propuesta.getEstado());
    verify(eventPublisher, never()).publishEvent(any());
    verify(propuestaRepository, never()).save(any());
  }

  @Test
  void
      actualizarEstado_aprobarUnaPropuestaYaAprobada_informaTransicionInvalidaNoNecesidadInactiva() {
    UUID id = UUID.randomUUID();
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(UUID.randomUUID());
    propuesta.aceptar("SISTEMA");
    when(propuestaRepository.findById(id)).thenReturn(Optional.of(propuesta));

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> service.actualizarEstado(id, EstadoPropuesta.APROBADA));

    assertEquals(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA, ex.getError());
    verify(necesidadRepository, never()).findById(any());
  }

  @Test
  void actualizarEstado_aprobadaConDonacionDeLaPropiaEntidad_lanzaDonacionASiMismo() {
    UUID id = UUID.randomUUID();
    UUID entidadYDonanteId = UUID.randomUUID();
    Necesidad necesidad = necesidadActivaDe(entidadYDonanteId);

    UUID donacionOriginalId = UUID.randomUUID();
    DonacionIndependiente independiente = mock(DonacionIndependiente.class);
    when(independiente.getId()).thenReturn(UUID.randomUUID());
    when(independiente.getDonacionOriginalId()).thenReturn(donacionOriginalId);
    Donacion donacion = mock(Donacion.class);
    when(donacion.getDonanteId()).thenReturn(entidadYDonanteId);

    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.agregarFragmentacion(independiente, 3);

    when(propuestaRepository.findById(id)).thenReturn(Optional.of(propuesta));
    when(necesidadRepository.findById(necesidad.getId())).thenReturn(Optional.of(necesidad));
    when(donacionRepository.findById(independiente.getId())).thenReturn(Optional.of(independiente));
    when(donacionesRepository.findById(donacionOriginalId)).thenReturn(Optional.of(donacion));

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> service.actualizarEstado(id, EstadoPropuesta.APROBADA));

    assertEquals(ErrorCatalog.DONACION_A_SI_MISMO, ex.getError());
    assertEquals(EstadoPropuesta.PENDIENTE, propuesta.getEstado());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void actualizarEstado_aprobadaConDonacionDeOtroDonante_esValida() {
    UUID id = UUID.randomUUID();
    Necesidad necesidad = necesidadActivaDe(UUID.randomUUID());

    UUID donacionOriginalId = UUID.randomUUID();
    DonacionIndependiente independiente = mock(DonacionIndependiente.class);
    when(independiente.getId()).thenReturn(UUID.randomUUID());
    when(independiente.getDonacionOriginalId()).thenReturn(donacionOriginalId);
    Donacion donacion = mock(Donacion.class);
    when(donacion.getDonanteId()).thenReturn(UUID.randomUUID());

    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.agregarFragmentacion(independiente, 3);

    when(propuestaRepository.findById(id)).thenReturn(Optional.of(propuesta));
    when(necesidadRepository.findById(necesidad.getId())).thenReturn(Optional.of(necesidad));
    when(donacionRepository.findById(independiente.getId())).thenReturn(Optional.of(independiente));
    when(donacionesRepository.findById(donacionOriginalId)).thenReturn(Optional.of(donacion));

    service.actualizarEstado(id, EstadoPropuesta.APROBADA);

    assertEquals(EstadoPropuesta.APROBADA, propuesta.getEstado());
    verify(eventPublisher, times(1)).publishEvent(any(PropuestaAprobada.class));
  }

  @Test
  void actualizarEstado_aprobadaConNecesidadSinEntidad_noEsAutoDonacion() {
    UUID id = UUID.randomUUID();
    Necesidad necesidad = necesidadActivaDe(null);
    DonacionIndependiente independiente = mock(DonacionIndependiente.class);
    when(independiente.getId()).thenReturn(UUID.randomUUID());
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.agregarFragmentacion(independiente, 3);
    when(propuestaRepository.findById(id)).thenReturn(Optional.of(propuesta));
    when(necesidadRepository.findById(necesidad.getId())).thenReturn(Optional.of(necesidad));

    service.actualizarEstado(id, EstadoPropuesta.APROBADA);

    assertEquals(EstadoPropuesta.APROBADA, propuesta.getEstado());
  }

  private static Necesidad necesidadActivaDe(UUID entidadId) {
    Necesidad necesidad = mock(Necesidad.class);
    when(necesidad.getId()).thenReturn(UUID.randomUUID());
    when(necesidad.isActiva()).thenReturn(true);
    when(necesidad.getEntidadId()).thenReturn(entidadId);
    return necesidad;
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
    // DTI-06: la asignación queda en las donaciones; la necesidad no cambia y no se guarda
    verify(necesidadRepository, never()).save(any());
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

  @Test
  void onPropuestaAprobada_conVariasFragmentaciones_debeResolverEntidadYPersonaUnaSolaVez() {
    Necesidad necesidad = mock(Necesidad.class);
    UUID necesidadId = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();
    UUID juridicaId = UUID.randomUUID();
    when(necesidadRepository.findById(necesidadId)).thenReturn(Optional.of(necesidad));
    when(necesidad.getEntidadId()).thenReturn(entidadId);

    EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
    when(entidad.juridicaId()).thenReturn(juridicaId);
    when(entidadesBeneficiariasRepository.findById(entidadId)).thenReturn(Optional.of(entidad));

    Juridica persona = mock(Juridica.class);
    Direccion direccionPersona = mock(Direccion.class);
    when(persona.getDireccion()).thenReturn(direccionPersona);
    when(personasRepository.findById(juridicaId)).thenReturn(Optional.of(persona));

    PosibleFragmentacion fragmentacion1 = mock(PosibleFragmentacion.class);
    PosibleFragmentacion fragmentacion2 = mock(PosibleFragmentacion.class);
    UUID donacionOriginalId1 = UUID.randomUUID();
    UUID donacionOriginalId2 = UUID.randomUUID();
    DonacionIndependiente donacionOriginal1 = mock(DonacionIndependiente.class);
    DonacionIndependiente donacionOriginal2 = mock(DonacionIndependiente.class);

    when(fragmentacion1.getDonacionOriginalId()).thenReturn(donacionOriginalId1);
    when(donacionRepository.findById(donacionOriginalId1))
        .thenReturn(Optional.of(donacionOriginal1));
    when(fragmentacion1.confirmar(necesidad, "actor")).thenReturn(donacionOriginal1);
    when(donacionOriginal1.getDonacionOriginalId()).thenReturn(donacionOriginalId1);

    when(fragmentacion2.getDonacionOriginalId()).thenReturn(donacionOriginalId2);
    when(donacionRepository.findById(donacionOriginalId2))
        .thenReturn(Optional.of(donacionOriginal2));
    when(fragmentacion2.confirmar(necesidad, "actor")).thenReturn(donacionOriginal2);
    when(donacionOriginal2.getDonacionOriginalId()).thenReturn(donacionOriginalId2);

    UUID donanteId = UUID.randomUUID();
    Donacion donacion = mock(Donacion.class);
    when(donacion.getDonanteId()).thenReturn(donanteId);
    when(donacionesRepository.findById(donacionOriginalId1)).thenReturn(Optional.of(donacion));
    when(donacionesRepository.findById(donacionOriginalId2)).thenReturn(Optional.of(donacion));
    Donante donante = mock(Donante.class);
    when(donantesRepository.findById(donanteId)).thenReturn(Optional.of(donante));

    PropuestaAprobada event =
        new PropuestaAprobada(
            UUID.randomUUID(), necesidadId, List.of(fragmentacion1, fragmentacion2), "actor");

    service.onPropuestaAprobada(event);

    verify(entidadesBeneficiariasRepository, times(1)).findById(entidadId);
    verify(personasRepository, times(1)).findById(juridicaId);
    verify(direccionMapper, times(1)).toDestinoEventoDTO(direccionPersona);
    verify(donacionesEventPublisher, times(2)).publicarDonacionAsignada(any());
  }

  @Test
  void onPropuestaAprobada_debeSolicitarEntregaAlBrokerConLosDatosDelBeneficiario() {
    Necesidad necesidad = mock(Necesidad.class);
    UUID necesidadId = UUID.randomUUID();
    UUID juridicaId = UUID.randomUUID();
    DestinoEventoDTO destino = mock(DestinoEventoDTO.class);
    prepararBeneficiario(necesidad, necesidadId, juridicaId, destino);

    PosibleFragmentacion fragmentacion = mock(PosibleFragmentacion.class);
    UUID donacionAsignadaId = UUID.randomUUID();
    prepararFragmentacion(fragmentacion, necesidad, donacionAsignadaId, 12.5, 0.3);

    service.onPropuestaAprobada(
        new PropuestaAprobada(UUID.randomUUID(), necesidadId, List.of(fragmentacion), "actor"));

    verify(logisticaBroker)
        .solicitarEntrega(
            argThat(
                (DatosEntregaLogistica datos) ->
                    datos.donacionIndependienteId().equals(donacionAsignadaId)
                        && datos.personaBeneficiariaId().equals(juridicaId)
                        && datos.destino() == destino
                        && datos.pesoTotalKG().equals(12.5)
                        && datos.volumenTotalM3().equals(0.3)
                        && datos.fecha() != null));
  }

  @Test
  void onPropuestaAprobada_conVariasFragmentaciones_debeSolicitarUnaEntregaPorCadaUna() {
    Necesidad necesidad = mock(Necesidad.class);
    UUID necesidadId = UUID.randomUUID();
    prepararBeneficiario(necesidad, necesidadId, UUID.randomUUID(), mock(DestinoEventoDTO.class));

    PosibleFragmentacion fragmentacion1 = mock(PosibleFragmentacion.class);
    PosibleFragmentacion fragmentacion2 = mock(PosibleFragmentacion.class);
    prepararFragmentacion(fragmentacion1, necesidad, UUID.randomUUID(), 1.0, 0.1);
    prepararFragmentacion(fragmentacion2, necesidad, UUID.randomUUID(), 2.0, 0.2);

    service.onPropuestaAprobada(
        new PropuestaAprobada(
            UUID.randomUUID(), necesidadId, List.of(fragmentacion1, fragmentacion2), "actor"));

    verify(logisticaBroker, times(2)).solicitarEntrega(any());
  }

  @Test
  void onPropuestaAprobada_sinDatosDelBeneficiario_noSolicitaEntregaAlBroker() {
    Necesidad necesidad = mock(Necesidad.class);
    UUID necesidadId = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();
    when(necesidadRepository.findById(necesidadId)).thenReturn(Optional.of(necesidad));
    when(necesidad.getEntidadId()).thenReturn(entidadId);
    when(entidadesBeneficiariasRepository.findById(entidadId)).thenReturn(Optional.empty());

    PosibleFragmentacion fragmentacion = mock(PosibleFragmentacion.class);
    prepararFragmentacion(fragmentacion, necesidad, UUID.randomUUID(), 1.0, 0.1);

    service.onPropuestaAprobada(
        new PropuestaAprobada(UUID.randomUUID(), necesidadId, List.of(fragmentacion), "actor"));

    verify(logisticaBroker, never()).solicitarEntrega(any());
  }

  @Test
  void onPropuestaAprobada_siElBrokerFalla_igualProcesaLasDemasFragmentaciones() {
    Necesidad necesidad = mock(Necesidad.class);
    UUID necesidadId = UUID.randomUUID();
    prepararBeneficiario(necesidad, necesidadId, UUID.randomUUID(), mock(DestinoEventoDTO.class));

    PosibleFragmentacion fragmentacion1 = mock(PosibleFragmentacion.class);
    PosibleFragmentacion fragmentacion2 = mock(PosibleFragmentacion.class);
    prepararFragmentacion(fragmentacion1, necesidad, UUID.randomUUID(), 1.0, 0.1);
    prepararFragmentacion(fragmentacion2, necesidad, UUID.randomUUID(), 2.0, 0.2);
    doThrow(new IllegalStateException("broker caído"))
        .doNothing()
        .when(logisticaBroker)
        .solicitarEntrega(any());

    service.onPropuestaAprobada(
        new PropuestaAprobada(
            UUID.randomUUID(), necesidadId, List.of(fragmentacion1, fragmentacion2), "actor"));

    verify(logisticaBroker, times(2)).solicitarEntrega(any());
    verify(necesidadRepository, never()).save(any());
  }

  /** Deja resolvible la entidad beneficiaria de la necesidad y su dirección de destino. */
  private void prepararBeneficiario(
      Necesidad necesidad, UUID necesidadId, UUID juridicaId, DestinoEventoDTO destino) {
    UUID entidadId = UUID.randomUUID();
    when(necesidadRepository.findById(necesidadId)).thenReturn(Optional.of(necesidad));
    when(necesidad.getEntidadId()).thenReturn(entidadId);

    EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
    when(entidad.juridicaId()).thenReturn(juridicaId);
    when(entidadesBeneficiariasRepository.findById(entidadId)).thenReturn(Optional.of(entidad));

    Juridica persona = mock(Juridica.class);
    Direccion direccionPersona = mock(Direccion.class);
    when(persona.getDireccion()).thenReturn(direccionPersona);
    when(personasRepository.findById(juridicaId)).thenReturn(Optional.of(persona));
    when(direccionMapper.toDestinoEventoDTO(direccionPersona)).thenReturn(destino);
  }

  /** Hace que confirmar la fragmentación devuelva una donación asignable con ese peso y volumen. */
  private void prepararFragmentacion(
      PosibleFragmentacion fragmentacion,
      Necesidad necesidad,
      UUID donacionAsignadaId,
      double peso,
      double volumen) {
    UUID donacionOriginalId = UUID.randomUUID();
    DonacionIndependiente donacionAsignada = mock(DonacionIndependiente.class);
    when(fragmentacion.getDonacionOriginalId()).thenReturn(donacionOriginalId);
    when(donacionRepository.findById(donacionOriginalId))
        .thenReturn(Optional.of(mock(DonacionIndependiente.class)));
    when(fragmentacion.confirmar(necesidad, "actor")).thenReturn(donacionAsignada);
    when(donacionAsignada.getId()).thenReturn(donacionAsignadaId);
    when(donacionAsignada.getDonacionOriginalId()).thenReturn(donacionOriginalId);
    when(donacionAsignada.getPesoTotal()).thenReturn(peso);
    when(donacionAsignada.getVolumenTotal()).thenReturn(volumen);
  }
}
