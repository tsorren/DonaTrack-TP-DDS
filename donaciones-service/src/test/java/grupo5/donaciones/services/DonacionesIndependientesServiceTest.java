package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.*;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.impl.DonacionesIndependientesService;
import grupo5.donaciones.services.mappers.DonacionIndependienteMapper;
import java.time.LocalDate;
import java.time.Month;
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
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private Donante testDonante;
  private Donacion testDonacionOriginal;

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
                              new grupo5
                                  .donaciones
                                  .dto
                                  .donacionesIndependientes
                                  .CambioEstadoDIResponseDTO(
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

  private DonacionIndependiente crearDonacionDePrueba() {
    Humana humana = new Humana("Juan", "Pérez", LocalDate.of(1990, Month.JANUARY, 1));
    testDonante = new Donante(humana.getId());
    testDonacionOriginal = new Donacion(testDonante.getId());

    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "Ropa de Invierno");
    Bien bien = new Bien("Abrigo", "abrigo.png", TEST_DATE.plusMonths(6), Estado.NUEVO, 1.0, 1.0);
    BienNormalizado bienNormalizado =
        new BienNormalizado(
            bien, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);

    ItemDonacionNormalizado itemNormalizado =
        new ItemDonacionNormalizado(testDonacionOriginal.getId(), bienNormalizado, 5);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(itemNormalizado.getBien(), 5);

    return new DonacionIndependiente(testDonacionOriginal.getId(), List.of(item));
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
    DonacionIndependiente donacion = crearDonacionDePrueba();
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
    DonacionIndependiente donacion = crearDonacionDePrueba();
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
    DonacionIndependiente donacion = crearDonacionDePrueba();
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
    DonacionIndependiente donacion = crearDonacionDePrueba();
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
    DonacionIndependiente donacion = crearDonacionDePrueba();
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
    DonacionIndependiente donacion = crearDonacionDePrueba();
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
    DonacionIndependiente donacion = crearDonacionDePrueba();
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
    DonacionIndependiente donacion = crearDonacionDePrueba();
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
    DonacionIndependiente donacion = crearDonacionDePrueba();
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
    DonacionIndependiente donacion = crearDonacionDePrueba();
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
    DonacionIndependiente donacion = crearDonacionDePrueba();
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    // EnDeposito a ENTREGADA es una transición inválida
    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGADA, null, null, null, null, null);

    assertThrows(BusinessStateException.class, () -> service.cambiarEstado(id, request, ACTOR));
    verify(repositoryMock, never()).save(any());
  }
}
