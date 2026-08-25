package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDIResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.AsignacionRealizada;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnDeposito;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnTraslado;
import grupo5.donaciones.models.entities.donacionesIndependientes.EntregaFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.Entregada;
import grupo5.donaciones.models.entities.donacionesIndependientes.ListaParaEntregar;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.Vencida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.impl.DonacionesIndependientesService;
import grupo5.donaciones.services.mappers.DonacionIndependienteMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class DonacionesIndependientesServiceTest {

  private IDonacionesIndependientesRepository repositoryMock;
  private INecesidadesRepository necesidadRepositoryMock;
  private DonacionIndependienteMapper mapperMock;
  private ApplicationEventPublisher eventPublisherMock;
  private DonacionesIndependientesService service;

  private static final String ACTOR = "SISTEMA";

  @BeforeEach
  void setUp() {
    repositoryMock = mock(IDonacionesIndependientesRepository.class);
    necesidadRepositoryMock = mock(INecesidadesRepository.class);
    mapperMock = mock(DonacionIndependienteMapper.class);
    eventPublisherMock = mock(ApplicationEventPublisher.class);

    when(mapperMock.toDTO(any(DonacionIndependiente.class)))
        .thenAnswer(
            invocation -> {
              DonacionIndependiente don = invocation.getArgument(0);
              return new DonacionIndependienteResponseDTO(
                  don.getId(),
                  don.getDonacionOriginalId(),
                  don.getDescripcion(),
                  don.getEstadoActual().getClass().getSimpleName(),
                  don.getFechaRegistro(),
                  don.getHistorial().stream()
                      .map(
                          c ->
                              new CambioEstadoDIResponseDTO(
                                  c.getEstadoAnterior() != null
                                      ? c.getEstadoAnterior().getClass().getSimpleName()
                                      : null,
                                  c.getEstadoNuevo().getClass().getSimpleName(),
                                  c.getTimestamp(),
                                  c.getJustificacion(),
                                  c.getActor()))
                      .toList(),
                  List.of(),
                  don.getCantidad());
            });

    service =
        new DonacionesIndependientesService(
            repositoryMock, necesidadRepositoryMock, mapperMock, eventPublisherMock);
  }

  @Test
  void cambiarEstado_DeberiaLanzarRecursoNoEncontradoException_CuandoNoExisteDonacion() {
    UUID id = UUID.randomUUID();
    when(repositoryMock.findById(id)).thenReturn(Optional.empty());

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, null, null, null, null, null);

    assertThrows(
        RecursoNoEncontradoException.class, () -> service.cambiarEstado(id, request, ACTOR));
    verify(repositoryMock, never()).save(any());
  }

  @Test
  void cambiarEstado_DeberiaTransicionarAAsignacionRealizada_CuandoEstadoActualEsEnDeposito() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    UUID id = donacion.getId();
    UUID necesidadId = UUID.randomUUID();
    Necesidad necesidad = mock(Necesidad.class);
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));
    when(necesidadRepositoryMock.findById(necesidadId)).thenReturn(Optional.of(necesidad));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, null, necesidadId, null, null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(AsignacionRealizada.class, donacion.getEstadoActual());
    assertEquals(id, response.id());
    assertEquals(necesidad, donacion.getAsignadaA());
    assertTrue(
        response.historial().stream().anyMatch(h -> "AsignacionRealizada".equals(h.estadoNuevo())));
    verify(repositoryMock, times(1)).save(donacion);
    verify(eventPublisherMock, atLeastOnce()).publishEvent(any(EventoDonacionIndependiente.class));
  }

  @Test
  void cambiarEstado_DeberiaLanzarRecursoNoEncontradoException_CuandoNecesidadNoExiste() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    UUID id = donacion.getId();
    UUID necesidadId = UUID.randomUUID();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));
    when(necesidadRepositoryMock.findById(necesidadId)).thenReturn(Optional.empty());

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, null, necesidadId, null, null, null);

    assertThrows(
        RecursoNoEncontradoException.class, () -> service.cambiarEstado(id, request, ACTOR));
    verify(repositoryMock, never()).save(any());
  }

  @Test
  void cambiarEstado_DeberiaTransicionarAVencida_CuandoEstadoActualEsEnDeposito() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.VENCIDA, null, null, null, null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(Vencida.class, donacion.getEstadoActual());
    assertEquals("Vencida", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
  }

  @Test
  void
      cambiarEstado_DeberiaTransicionarAListaParaEntregar_CuandoEstadoActualEsAsignacionRealizada() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    donacion.asignar(ACTOR, null);
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.LISTA_PARA_ENTREGAR, null, null, null, null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(ListaParaEntregar.class, donacion.getEstadoActual());
    assertEquals("ListaParaEntregar", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
  }

  @Test
  void cambiarEstado_DeberiaTransicionarAEnTraslado_CuandoEstadoActualEsListaParaEntregar() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    donacion.asignar(ACTOR, null);
    donacion.planificarRuta(ACTOR);
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.EN_TRASLADO, null, null, "http://mapa/ruta-1", null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(EnTraslado.class, donacion.getEstadoActual());
    assertEquals("EnTraslado", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
    verify(eventPublisherMock, atLeastOnce()).publishEvent(any(EventoDonacionIndependiente.class));
  }

  @Test
  void cambiarEstado_DeberiaTransicionarAEntregada_CuandoEstadoActualEsEnTraslado() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    donacion.asignar(ACTOR, null);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGADA, null, null, null, "ABC-123", null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(Entregada.class, donacion.getEstadoActual());
    assertEquals("Entregada", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
    verify(eventPublisherMock, atLeastOnce()).publishEvent(any(EventoDonacionIndependiente.class));
  }

  @Test
  void
      cambiarEstado_DeberiaTransicionarAEntregaFallida_CuandoEstadoActualEsEnTrasladoYJustificacionEsValidaYNoEsReplanificable() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    donacion.asignar(ACTOR, null);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGA_FALLIDA, "Dirección incorrecta", null, null, null, false);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(EntregaFallida.class, donacion.getEstadoActual());
    assertEquals("EntregaFallida", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
    verify(eventPublisherMock, atLeastOnce()).publishEvent(any(EventoDonacionIndependiente.class));
  }

  @Test
  void
      cambiarEstado_DeberiaTransicionarAAsignacionRealizada_CuandoEstadoActualEsEnTrasladoYEsReplanificable() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    donacion.asignar(ACTOR, null);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGA_FALLIDA, "Dirección incorrecta", null, null, null, true);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(AsignacionRealizada.class, donacion.getEstadoActual());
    assertEquals("AsignacionRealizada", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
    verify(eventPublisherMock, atLeastOnce()).publishEvent(any(EventoDonacionIndependiente.class));
  }

  @Test
  void
      cambiarEstado_DeberiaLanzarIllegalArgumentException_CuandoEstadoActualEsEnTrasladoYJustificacionEsInvalida() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    donacion.asignar(ACTOR, null);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGA_FALLIDA, "", null, null, null, null);

    assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(id, request, ACTOR));
    verify(repositoryMock, never()).save(any());
  }

  @Test
  void cambiarEstado_DeberiaTransicionarAEnDeposito_CuandoEstadoActualEsEntregaFallida() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    donacion.asignar(ACTOR, null);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    donacion.registrarFalla("motivo test", ACTOR);
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.EN_DEPOSITO, null, null, null, null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(EnDeposito.class, donacion.getEstadoActual());
    assertEquals("EnDeposito", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
  }

  @Test
  void cambiarEstado_DeberiaLanzarBusinessStateException_CuandoTransicionEsInvalida() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGADA, null, null, null, null, null);

    assertThrows(BusinessStateException.class, () -> service.cambiarEstado(id, request, ACTOR));
    verify(repositoryMock, never()).save(any());
  }
}
