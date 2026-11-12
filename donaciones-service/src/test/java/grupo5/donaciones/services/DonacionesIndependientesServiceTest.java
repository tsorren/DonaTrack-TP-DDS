package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDIResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.FiltroDonacionesIndependientes;
import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.AsignacionRealizada;
import grupo5.donaciones.models.entities.donacionesIndependientes.DatosDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnDeposito;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnTraslado;
import grupo5.donaciones.models.entities.donacionesIndependientes.EntregaFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.Entregada;
import grupo5.donaciones.models.entities.donacionesIndependientes.ListaParaEntregar;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.Vencida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.DestinoAsignacion;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.repositories.impl.DonacionesIndependientesRepositoryEnMemoria;
import grupo5.donaciones.services.impl.DonacionesIndependientesService;
import grupo5.donaciones.services.mappers.DonacionIndependienteMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class DonacionesIndependientesServiceTest {

  private IDonacionesIndependientesRepository repositoryMock;
  private IDonacionesRepository donacionesRepositoryMock;
  private INecesidadesRepository necesidadRepositoryMock;
  private ISubcategoriasRepository subcategoriasRepositoryMock;
  private DonacionIndependienteMapper mapperMock;
  private ApplicationEventPublisher eventPublisherMock;
  private DonacionesIndependientesService service;
  private DonacionesIndependientesRepositoryEnMemoria enMemoria;
  private DonacionesIndependientesService servicioConFiltros;

  private static final String ACTOR = "SISTEMA";

  @BeforeEach
  void setUp() {
    repositoryMock = mock(IDonacionesIndependientesRepository.class);
    donacionesRepositoryMock = mock(IDonacionesRepository.class);
    necesidadRepositoryMock = mock(INecesidadesRepository.class);
    subcategoriasRepositoryMock = mock(ISubcategoriasRepository.class);
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
                  don.getCantidad(),
                  don.getEntidadBeneficiariaId());
            });

    when(mapperMock.toDTOs(anyList()))
        .thenAnswer(
            invocation ->
                invocation.<List<DonacionIndependiente>>getArgument(0).stream()
                    .map(mapperMock::toDTO)
                    .toList());

    service =
        new DonacionesIndependientesService(
            repositoryMock, necesidadRepositoryMock, mapperMock, eventPublisherMock);
    // R11-01: los filtros se resuelven en el repositorio; acá, el de memoria con las mismas
    // donaciones y subcategorías simuladas.
    enMemoria =
        new DonacionesIndependientesRepositoryEnMemoria(
            donacionesRepositoryMock, subcategoriasRepositoryMock);
    servicioConFiltros =
        new DonacionesIndependientesService(
            enMemoria, necesidadRepositoryMock, mapperMock, eventPublisherMock);
  }

  @Test
  void registrarCantidadRecibida_guardaLoQueNoLlego() {
    DonacionIndependiente donacion = mock(DonacionIndependiente.class);
    UUID id = UUID.randomUUID();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    service.registrarCantidadRecibida(id, 7.0);

    verify(donacion).registrarCantidadRecibida(7.0);
    verify(repositoryMock).save(donacion);
  }

  @Test
  void registrarCantidadRecibida_sinDonacion_lanzaRecursoNoEncontrado() {
    UUID id = UUID.randomUUID();
    when(repositoryMock.findById(id)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNoEncontradoException.class, () -> service.registrarCantidadRecibida(id, 7.0));
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
    when(necesidad.admiteAsignaciones(any(LocalDate.class))).thenReturn(true);
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));
    when(necesidadRepositoryMock.findById(necesidadId)).thenReturn(Optional.of(necesidad));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, null, necesidadId, null, null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(AsignacionRealizada.class, donacion.getEstadoActual());
    assertEquals(id, response.id());
    assertEquals(necesidad.getId(), donacion.getNecesidadId());
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

    assertThrows(ValidationException.class, () -> service.cambiarEstado(id, request, ACTOR));
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

  @Test
  void obtener_DeberiaRetornarDTO_CuandoExiste() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    DonacionIndependienteResponseDTO resultado = service.obtener(id);

    assertEquals(id, resultado.id());
    assertEquals("EnDeposito", resultado.estadoActual());
  }

  @Test
  void obtener_DeberiaLanzarRecursoNoEncontradoException_CuandoNoExiste() {
    UUID id = UUID.randomUUID();
    when(repositoryMock.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(id));
  }

  @Test
  void vencer_siUnaDonacionFueModificadaAlMismoTiempo_igualVenceLasDemas() {
    DonacionIndependiente conflicto = mock(DonacionIndependiente.class);
    DonacionIndependiente otra = mock(DonacionIndependiente.class);
    when(conflicto.estaVencida()).thenReturn(true);
    when(otra.estaVencida()).thenReturn(true);
    when(repositoryMock.findEnDeposito()).thenReturn(List.of(conflicto, otra));
    when(repositoryMock.save(conflicto))
        .thenThrow(
            new BusinessStateException(
                ErrorCatalog.DONACION_INDEPENDIENTE_MODIFICADA_CONCURRENTEMENTE));

    service.vencer();

    verify(repositoryMock).save(otra);
  }

  @Test
  void vencer_siElGuardadoFallaPorOtroMotivo_propagaLaExcepcion() {
    DonacionIndependiente donacion = mock(DonacionIndependiente.class);
    when(donacion.estaVencida()).thenReturn(true);
    when(repositoryMock.findEnDeposito()).thenReturn(List.of(donacion));
    when(repositoryMock.save(donacion))
        .thenThrow(new BusinessStateException(ErrorCatalog.NECESIDAD_INACTIVA));

    assertThrows(BusinessStateException.class, () -> service.vencer());
  }

  // ===================== obtenerConFiltros (HB-03, HB-04) =====================

  private static FiltroDonacionesIndependientes filtro(
      UUID categoriaId, UUID entidadId, LocalDate desde, LocalDate hasta) {
    return new FiltroDonacionesIndependientes(
        null, null, categoriaId, null, entidadId, desde, hasta, null);
  }

  private static DonacionIndependiente registradaEl(LocalDate fecha) {
    DonacionIndependiente base = DonacionIndependienteMother.enDeposito(1);
    return new DonacionIndependiente(
        new DatosDonacionIndependiente(
            base.getId(),
            base.getDonacionOriginalId(),
            new EnDeposito(),
            fecha.atTime(12, 0),
            null,
            null),
        base.getItems(),
        List.of());
  }

  private static final ParametrosListado DE_LA_MAS_ANTIGUA =
      new ParametrosListado(null, null, null, "ANTIGUAS");

  private void guardar(DonacionIndependiente... donaciones) {
    for (DonacionIndependiente d : donaciones) {
      enMemoria.save(d);
    }
  }

  private List<UUID> idsFiltrados(FiltroDonacionesIndependientes filtro) {
    return servicioConFiltros.obtenerConFiltros(filtro, DE_LA_MAS_ANTIGUA).elementos().stream()
        .map(DonacionIndependienteResponseDTO::id)
        .toList();
  }

  @Test
  void obtenerConFiltros_porCategoria_devuelveSoloLasDeSusSubcategorias() {
    UUID categoriaId = UUID.randomUUID();
    Subcategoria deLaCategoria = new Subcategoria(categoriaId, "Camperas");
    Subcategoria deOtra = new Subcategoria(UUID.randomUUID(), "Fideos");
    DonacionIndependiente esperada =
        DonacionIndependienteMother.crearParaSubcategoria(deLaCategoria.getId(), 1);
    DonacionIndependiente otra =
        DonacionIndependienteMother.crearParaSubcategoria(deOtra.getId(), 1);
    when(subcategoriasRepositoryMock.findAll()).thenReturn(List.of(deLaCategoria, deOtra));
    guardar(esperada, otra);

    assertEquals(List.of(esperada.getId()), idsFiltrados(filtro(categoriaId, null, null, null)));
  }

  @Test
  void obtenerConFiltros_porEntidad_devuelveSoloLasAsignadasAEsaEntidad() {
    UUID entidadId = UUID.randomUUID();
    DonacionIndependiente asignada = DonacionIndependienteMother.enDeposito(1);
    asignada.asignarDestino(new DestinoAsignacion(UUID.randomUUID(), null, entidadId));
    DonacionIndependiente deOtraEntidad = DonacionIndependienteMother.enDeposito(1);
    deOtraEntidad.asignarDestino(new DestinoAsignacion(UUID.randomUUID(), null, UUID.randomUUID()));
    DonacionIndependiente sinAsignar = DonacionIndependienteMother.enDeposito(1);
    guardar(asignada, deOtraEntidad, sinAsignar);

    assertEquals(List.of(asignada.getId()), idsFiltrados(filtro(null, entidadId, null, null)));
  }

  @Test
  void obtenerConFiltros_porRangoDeFechas_esInclusivoEnAmbosExtremos() {
    LocalDate desde = LocalDate.of(2026, 11, 1);
    LocalDate hasta = LocalDate.of(2026, 11, 5);
    DonacionIndependiente antes = registradaEl(desde.minusDays(1));
    DonacionIndependiente enDesde = registradaEl(desde);
    DonacionIndependiente enHasta = registradaEl(hasta);
    DonacionIndependiente despues = registradaEl(hasta.plusDays(1));
    guardar(antes, enDesde, enHasta, despues);

    assertEquals(
        List.of(enDesde.getId(), enHasta.getId()), idsFiltrados(filtro(null, null, desde, hasta)));
  }

  @Test
  void obtenerConFiltros_conSoloDesde_noAcotaElFinal() {
    LocalDate desde = LocalDate.of(2026, 11, 1);
    DonacionIndependiente antes = registradaEl(desde.minusDays(1));
    DonacionIndependiente despues = registradaEl(desde.plusDays(30));
    guardar(antes, despues);

    assertEquals(List.of(despues.getId()), idsFiltrados(filtro(null, null, desde, null)));
  }

  @Test
  void obtenerConFiltros_conDesdePosteriorAHasta_lanzaValidationException() {
    FiltroDonacionesIndependientes invertido =
        filtro(null, null, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1));

    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> service.obtenerConFiltros(invertido, DE_LA_MAS_ANTIGUA));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, ex.getError());
    verify(repositoryMock, never()).buscar(any());
  }

  @Test
  void obtenerConFiltros_porDonanteEstadoYSubcategoria_combinaLosTres() {
    UUID donanteId = UUID.randomUUID();
    UUID subcategoriaId = UUID.randomUUID();
    Donacion delDonante = DonacionMother.simple(donanteId);
    Donacion deOtro = DonacionMother.simple(UUID.randomUUID());
    DonacionIndependiente esperada = deLaDonacion(delDonante, subcategoriaId);
    DonacionIndependiente deOtroDonante = deLaDonacion(deOtro, subcategoriaId);
    DonacionIndependiente otraSubcategoria = deLaDonacion(delDonante, UUID.randomUUID());
    DonacionIndependiente otroEstado = deLaDonacion(delDonante, subcategoriaId);
    otroEstado.asignar(ACTOR, new DestinoAsignacion(UUID.randomUUID(), null, UUID.randomUUID()));
    when(donacionesRepositoryMock.findAll()).thenReturn(List.of(delDonante, deOtro));
    guardar(esperada, deOtroDonante, otraSubcategoria, otroEstado);

    assertEquals(
        List.of(esperada.getId()),
        idsFiltrados(
            new FiltroDonacionesIndependientes(
                List.of(TipoEstadoDonacion.EN_DEPOSITO),
                subcategoriaId,
                null,
                donanteId,
                null,
                null,
                null,
                null)));
  }

  @Test
  void obtenerConFiltros_porNecesidad_devuelveLasQueLaCubren() {
    UUID necesidadId = UUID.randomUUID();
    UUID subcategoriaId = UUID.randomUUID();
    DonacionIndependiente cubre =
        DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 1);
    cubre.asignar(ACTOR, new DestinoAsignacion(necesidadId, null, UUID.randomUUID()));
    DonacionIndependiente otra =
        DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 1);
    otra.asignar(ACTOR, new DestinoAsignacion(UUID.randomUUID(), null, UUID.randomUUID()));
    DonacionIndependiente sinAsignar =
        DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 1);
    guardar(cubre, otra, sinAsignar);

    assertEquals(
        List.of(cubre.getId()),
        idsFiltrados(
            new FiltroDonacionesIndependientes(
                null, null, null, null, null, null, null, necesidadId)));
  }

  private static DonacionIndependiente deLaDonacion(Donacion original, UUID subcategoriaId) {
    DonacionIndependiente base =
        DonacionIndependienteMother.crearParaSubcategoria(subcategoriaId, 1);
    return new DonacionIndependiente(original.getId(), base.getItems());
  }

  @Test
  void obtenerConFiltros_sinFiltros_devuelveTodas() {
    DonacionIndependiente una = DonacionIndependienteMother.enDeposito(1);
    DonacionIndependiente otra = DonacionIndependienteMother.enDeposito(2);
    guardar(una, otra);

    // Las dos pueden tener la misma fecha de registro: el desempate por id no sigue la carga
    assertEquals(
        Set.of(una.getId(), otra.getId()),
        Set.copyOf(
            idsFiltrados(
                new FiltroDonacionesIndependientes(
                    null, null, null, null, null, null, null, null))));
    verify(subcategoriasRepositoryMock, never()).findAll();
  }

  @Test
  void obtenerConFiltros_devuelveUnaPaginaConElTotalDeLasQueCumplen_R1101() {
    DonacionIndependiente primera = registradaEl(LocalDate.of(2026, 11, 1));
    DonacionIndependiente segunda = registradaEl(LocalDate.of(2026, 11, 2));
    DonacionIndependiente tercera = registradaEl(LocalDate.of(2026, 11, 3));
    guardar(primera, segunda, tercera);

    var pagina =
        servicioConFiltros.obtenerConFiltros(
            new FiltroDonacionesIndependientes(null, null, null, null, null, null, null, null),
            new ParametrosListado(1, 2, null, null));

    // Por defecto, la más reciente primero: la segunda página de 2 trae solo la más antigua
    assertEquals(3, pagina.total());
    assertEquals(
        List.of(primera.getId()),
        pagina.elementos().stream().map(DonacionIndependienteResponseDTO::id).toList());
  }

  @Test
  void obtenerConFiltros_conVariosEstados_devuelveLasDeCualquieraDeEllos_R1101() {
    DonacionIndependiente enDeposito = DonacionIndependienteMother.enDeposito(1);
    DonacionIndependiente asignada = DonacionIndependienteMother.enDeposito(1);
    asignada.asignar(ACTOR, new DestinoAsignacion(UUID.randomUUID(), null, UUID.randomUUID()));
    DonacionIndependiente conRuta = DonacionIndependienteMother.enDeposito(1);
    conRuta.asignar(ACTOR, new DestinoAsignacion(UUID.randomUUID(), null, UUID.randomUUID()));
    conRuta.planificarRuta(ACTOR);
    guardar(enDeposito, asignada, conRuta);

    assertEquals(
        Set.of(enDeposito.getId(), asignada.getId()),
        Set.copyOf(
            idsFiltrados(
                new FiltroDonacionesIndependientes(
                    List.of(
                        TipoEstadoDonacion.EN_DEPOSITO, TipoEstadoDonacion.ASIGNACION_REALIZADA),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null))));
  }

  @Test
  void cambiarEstado_aAsignacionRealizada_rechazaUnaNecesidadDadaDeBaja_HB19() {
    DonacionIndependiente donacion = DonacionIndependienteMother.enDeposito(5);
    NecesidadExtraordinaria necesidad = NecesidadMother.extraordinaria(UUID.randomUUID(), 10);
    necesidad.desactivar();
    when(repositoryMock.findById(donacion.getId())).thenReturn(Optional.of(donacion));
    when(necesidadRepositoryMock.findById(necesidad.getId())).thenReturn(Optional.of(necesidad));
    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, null, necesidad.getId(), null, null, null);
    UUID id = donacion.getId();

    BusinessStateException e =
        assertThrows(BusinessStateException.class, () -> service.cambiarEstado(id, request, ACTOR));

    assertEquals(ErrorCatalog.NECESIDAD_INACTIVA, e.getError());
    assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacion.getEstadoActual().getTipo());
    verify(repositoryMock, never()).save(any());
  }
}
