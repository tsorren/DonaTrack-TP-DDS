package grupo5.logistica.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.entregas.AdjuntarFotoRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaRequestDTO;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.entregas.EntregaMapaPublicoResponseDTO;
import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import grupo5.logistica.dto.entregas.SeguimientoEntregaResponseDTO;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.services.impl.EntregasService;
import grupo5.logistica.services.mappers.DireccionMapper;
import grupo5.logistica.services.mappers.EntregaMapper;
import grupo5.logistica.testutils.EntregaMother;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntregaServiceTest {

  private IEntregasRepository entregasRepository;
  private EntregaMapper entregaMapper;
  private ComunicadorEventosLogistica comunicadorEventos;
  private EntregasService entregasService;
  private IRutasRepository rutasRepository;
  private ICamionRepository camionRepository;

  @BeforeEach
  void setUp() {
    entregasRepository = mock(IEntregasRepository.class);
    rutasRepository = mock(IRutasRepository.class);
    camionRepository = mock(ICamionRepository.class);
    entregaMapper = new EntregaMapper(new DireccionMapper());
    comunicadorEventos = mock(ComunicadorEventosLogistica.class);

    entregasService =
        new EntregasService(
            entregasRepository,
            rutasRepository,
            camionRepository,
            entregaMapper,
            comunicadorEventos);
  }

  // ===================== crear() =====================

  @Test
  void crear_deberiaGuardarYDevolverDTO_cuandoDatosValidos() {
    DireccionDTO destino =
        new DireccionDTO("Calle Falsa", 123, 4, "B", "C1000", "CABA", "Buenos Aires", "Argentina");
    CrearEntregaRequestDTO request =
        new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), destino, 10f, 2f);

    when(entregasRepository.save(any(Entrega.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    EntregaResponseDTO resultado = entregasService.crear(request);

    verify(entregasRepository).save(any(Entrega.class));
    assertNotNull(resultado);
    assertEquals(request.idDonacion(), resultado.idDonacion());
    assertEquals(request.idBeneficiaria(), resultado.idBeneficiaria());
    assertEquals(EstadoEntrega.PENDIENTE, resultado.estadoActual());
  }

  @Test
  void crear_deberiaUsarGeocodificadorCuandoNoVienenCoordenadas() {
    IGeocodificadorDestino geocodificador = mock(IGeocodificadorDestino.class);
    when(geocodificador.geocodificar(any()))
        .thenReturn(Optional.of(new CoordenadasGeograficas(-34.6037, -58.3816)));

    EntregasService serviceConGeo =
        new EntregasService(
            entregasRepository,
            rutasRepository,
            camionRepository,
            entregaMapper,
            comunicadorEventos,
            null,
            geocodificador);

    DireccionDTO destino =
        new DireccionDTO(
            "Calle Falsa", 123, null, null, "C1000", "CABA", "Buenos Aires", "Argentina");
    CrearEntregaRequestDTO request =
        new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), destino, 10f, 2f);

    when(entregasRepository.save(any(Entrega.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    EntregaResponseDTO resultado = serviceConGeo.crear(request);

    assertEquals(-34.6037, resultado.destino().latitud());
    assertEquals(-58.3816, resultado.destino().longitud());
  }

  @Test
  void crear_deberiaLanzarExcepcion_cuandoMapperDevuelveNull() {
    assertThrows(ValidationException.class, () -> entregasService.crear(null));
    verify(entregasRepository, never()).save(any());
  }

  @Test
  void crear_deberiaLanzarConflicto_cuandoYaExisteEntregaParaLaDonacion() {
    DireccionDTO destino =
        new DireccionDTO("Calle Falsa", 123, 4, "B", "C1000", "CABA", "Buenos Aires", "Argentina");
    CrearEntregaRequestDTO request =
        new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), destino, 10f, 2f);

    when(entregasRepository.existsByIdDonacion(request.idDonacion())).thenReturn(true);

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> entregasService.crear(request));
    assertEquals(ErrorCatalog.ENTREGA_DONACION_DUPLICADA, ex.getError());
    verify(entregasRepository, never()).save(any());
  }

  // ===================== listar() =====================

  @Test
  void listar_deberiaRetornarListaDeEntregas() {
    Entrega entrega = EntregaMother.pendiente();

    when(entregasRepository.findAll()).thenReturn(List.of(entrega));

    List<EntregaResponseDTO> resultado = entregasService.listar();

    assertEquals(1, resultado.size());
    assertEquals(entrega.getId(), resultado.getFirst().id());
    assertEquals(entrega.getIdDonacion(), resultado.getFirst().idDonacion());
  }

  @Test
  void listar_deberiaRetornarListaVacia_cuandoNoHayEntregas() {
    when(entregasRepository.findAll()).thenReturn(List.of());

    List<EntregaResponseDTO> resultado = entregasService.listar();

    assertTrue(resultado.isEmpty());
  }

  // ===================== listarMapaPublico() (RF-02) =====================

  @Test
  void listarMapaPublico_deberiaRetornarSoloEntregadasConCoordenadasValidasYSanitizadas() {
    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion dirConCoords =
        new Direccion(
            "Av. Medrano",
            951,
            2,
            "A",
            "C1179",
            caba,
            new CoordenadasGeograficas(-34.5986, -58.4201));
    Direccion dirSinCoords = new Direccion("Av. Rivadavia", 1200, null, null, "C1033", caba);

    Entrega entregadaConCoords =
        new Entrega(UUID.randomUUID(), UUID.randomUUID(), dirConCoords, 15.0f, 1.5f);
    entregadaConCoords.asignarRuta(UUID.randomUUID());
    entregadaConCoords.iniciarRuta("Chofer");
    entregadaConCoords.confirmarEntrega("Comedor", "https://storage.donatrack.org/foto1.jpg");

    Entrega entregadaSinCoords =
        new Entrega(UUID.randomUUID(), UUID.randomUUID(), dirSinCoords, 20.0f, 2.0f);
    entregadaSinCoords.asignarRuta(UUID.randomUUID());
    entregadaSinCoords.iniciarRuta("Chofer");
    entregadaSinCoords.confirmarEntrega("Comedor", "https://storage.donatrack.org/foto2.jpg");

    when(entregasRepository.findByEstado(EstadoEntrega.ENTREGADA))
        .thenReturn(List.of(entregadaConCoords, entregadaSinCoords));

    List<EntregaMapaPublicoResponseDTO> resultado = entregasService.listarMapaPublico();

    assertEquals(1, resultado.size());
    EntregaMapaPublicoResponseDTO item = resultado.getFirst();
    assertEquals(entregadaConCoords.getId(), item.idEntrega());
    assertEquals(-34.5986, item.latitud());
    assertEquals(-58.4201, item.longitud());
    assertEquals("CABA", item.localidad());
    assertEquals("Buenos Aires", item.provincia());
    assertEquals("Argentina", item.pais());
    assertNotNull(item.fechaEntrega());
    assertEquals(15.0f, item.pesoTotalKG());
    assertEquals(1.5f, item.volumenTotalM3());
  }

  // ===================== obtenerPorId() =====================

  @Test
  void obtenerPorId_deberiaRetornarEntrega_cuandoExiste() {
    Entrega entrega = EntregaMother.pendiente();
    UUID id = entrega.getId();

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));

    EntregaResponseDTO resultado = entregasService.obtenerPorId(id);

    assertEquals(id, resultado.id());
    assertEquals(entrega.getIdDonacion(), resultado.idDonacion());
  }

  @Test
  void obtenerPorId_deberiaLanzarExcepcion_cuandoNoExiste() {
    UUID id = UUID.randomUUID();

    when(entregasRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> entregasService.obtenerPorId(id));
  }

  // ===================== Seguimiento RF-09 y RF-14 =====================

  @Test
  void obtenerSeguimientoPorDonacion_sinRutaAsignada_retornaUltimaActualizacionNullSiNoHayMarcas() {
    Entrega entrega = EntregaMother.pendiente();
    UUID idDonacion = entrega.getIdDonacion();

    when(entregasRepository.findByIdDonacion(idDonacion)).thenReturn(Optional.of(entrega));

    SeguimientoEntregaResponseDTO dto = entregasService.obtenerSeguimientoPorDonacion(idDonacion);

    assertEquals(entrega.getId(), dto.idEntrega());
    assertEquals(idDonacion, dto.idDonacion());
    assertEquals(EstadoEntrega.PENDIENTE, dto.estadoEntrega());
    assertFalse(dto.rutaAsignada());
    assertNull(dto.idRuta());
    assertNull(dto.ordenVisita());
    assertEquals(0, dto.totalParadas());
    assertEquals(0, dto.paradasEntregadas());
    assertEquals(0, dto.paradasProcesadas());
    assertNull(dto.ultimaActualizacion());
    assertTrue(dto.paradas().isEmpty());
  }

  @Test
  void obtenerSeguimientoPorDonacion_conRutaEnTraslado_distingueEntregadasDeProcesadas() {
    Entrega e1Entregada = EntregaMother.pendiente();
    Entrega e2NoRecibida = EntregaMother.pendiente();
    Entrega e3Consultada = EntregaMother.pendiente();

    Camion camion = new Camion("AB123CD", 50f, 2000f, 3f);
    Ruta ruta = new Ruta(LocalDate.of(2026, 10, 22), UUID.randomUUID(), camion.getId());
    ruta.agregarEntrega(e1Entregada.getId());
    ruta.agregarEntrega(e2NoRecibida.getId());
    ruta.agregarEntrega(e3Consultada.getId());

    e1Entregada.asignarRuta(ruta.getId());
    e2NoRecibida.asignarRuta(ruta.getId());
    e3Consultada.asignarRuta(ruta.getId());

    ruta.iniciarRuta();
    e1Entregada.iniciarRuta("Chofer");
    e2NoRecibida.iniciarRuta("Chofer");
    e3Consultada.iniciarRuta("Chofer");

    e1Entregada.confirmarEntrega("Receptor 1", "https://storage.donatrack.org/e1.jpg");
    e2NoRecibida.negarEntrega("Chofer", "Ausente", true);

    when(entregasRepository.findByIdDonacion(e3Consultada.getIdDonacion()))
        .thenReturn(Optional.of(e3Consultada));
    when(rutasRepository.findById(ruta.getId())).thenReturn(Optional.of(ruta));
    when(camionRepository.findById(camion.getId())).thenReturn(Optional.of(camion));
    when(entregasRepository.findById(e1Entregada.getId())).thenReturn(Optional.of(e1Entregada));
    when(entregasRepository.findById(e2NoRecibida.getId())).thenReturn(Optional.of(e2NoRecibida));

    SeguimientoEntregaResponseDTO dto =
        entregasService.obtenerSeguimientoPorDonacion(e3Consultada.getIdDonacion());

    assertTrue(dto.rutaAsignada());
    assertEquals(ruta.getId(), dto.idRuta());
    assertEquals(EstadoRuta.EN_TRASLADO, dto.estadoRuta());
    assertEquals("AB123CD", dto.patenteCamion());
    assertEquals(3, dto.ordenVisita());
    assertEquals(3, dto.totalParadas());
    assertEquals(1, dto.paradasEntregadas());
    assertEquals(2, dto.paradasProcesadas());
    assertNotNull(dto.ultimaActualizacion());
    assertEquals(3, dto.paradas().size());
    assertFalse(dto.paradas().get(0).esEntregaConsultada());
    assertFalse(dto.paradas().get(1).esEntregaConsultada());
    assertTrue(dto.paradas().get(2).esEntregaConsultada());
  }

  @Test
  void obtenerSeguimientoPorDonacion_lanza404CuandoNoExiste() {
    UUID idDonacion = UUID.randomUUID();
    when(entregasRepository.findByIdDonacion(idDonacion)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNoEncontradoException.class,
        () -> entregasService.obtenerSeguimientoPorDonacion(idDonacion));
  }

  @Test
  void listarSeguimientoPorBeneficiaria_retornaSeguimientosDelBeneficiario() {
    UUID idBeneficiaria = UUID.randomUUID();
    Entrega entrega = EntregaMother.pendiente();
    when(entregasRepository.findByIdBeneficiaria(idBeneficiaria)).thenReturn(List.of(entrega));

    List<SeguimientoEntregaResponseDTO> resultado =
        entregasService.listarSeguimientoPorBeneficiaria(idBeneficiaria);

    assertEquals(1, resultado.size());
    assertEquals(entrega.getId(), resultado.getFirst().idEntrega());
  }

  // ===================== adjuntarFotoRecepcion() =====================

  @Test
  void adjuntarFotoRecepcion_deberiaGuardarFoto() {
    Entrega entrega = EntregaMother.pendiente();
    entrega.asignarRuta(UUID.randomUUID());
    entrega.iniciarRuta("Chofer");
    entrega.confirmarEntrega("Actor", "https://storage.donatrack.org/inicial.jpg");
    UUID id = entrega.getId();

    AdjuntarFotoRecepcionRequestDTO request = new AdjuntarFotoRecepcionRequestDTO("foto.jpg");

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));
    when(entregasRepository.save(entrega)).thenReturn(entrega);

    EntregaResponseDTO resultado = entregasService.adjuntarFotoRecepcion(id, request);

    verify(entregasRepository).save(entrega);
    assertEquals(
        List.of("https://storage.donatrack.org/inicial.jpg", "foto.jpg"),
        resultado.fotosRecepcion());
  }

  // ===================== cambiarEstado() =====================

  @Test
  void cambiarEstado_deberiaConfirmarRecepcion_cuandoEstadoEsEntregadaConEvidenciaFotografica() {
    UUID rutaId = UUID.randomUUID();
    UUID camionId = UUID.randomUUID();
    Entrega entrega = EntregaMother.pendiente();
    entrega.asignarRuta(rutaId);
    entrega.iniciarRuta("Chofer");
    UUID id = entrega.getId();

    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(
            EstadoEntrega.ENTREGADA,
            "actor",
            null,
            null,
            List.of("https://storage.donatrack.org/evidencia-1.jpg"));

    Ruta ruta = mock(Ruta.class);
    Camion camion = mock(Camion.class);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));
    when(rutasRepository.findById(rutaId)).thenReturn(Optional.of(ruta));
    when(ruta.getCamionId()).thenReturn(camionId);
    when(camionRepository.findById(camionId)).thenReturn(Optional.of(camion));
    when(entregasRepository.save(entrega)).thenReturn(entrega);

    EntregaResponseDTO resultado = entregasService.cambiarEstado(id, request);

    assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstadoActual());
    assertEquals(EstadoEntrega.ENTREGADA, resultado.estadoActual());
    assertEquals(
        List.of("https://storage.donatrack.org/evidencia-1.jpg"), resultado.fotosRecepcion());
    verify(entregasRepository).save(entrega);
    verify(comunicadorEventos).comunicarEntregaExitosa(any(EntregaConfirmada.class), eq(camion));
  }

  @Test
  void cambiarEstado_deberiaRechazarConfirmacionSinFotoYNoPublicarEvento() {
    UUID rutaId = UUID.randomUUID();
    Entrega entrega = EntregaMother.pendiente();
    entrega.asignarRuta(rutaId);
    entrega.iniciarRuta("Chofer");
    UUID id = entrega.getId();

    CambioEstadoEntregaRequestDTO requestSinFoto =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.ENTREGADA, "actor", null, null);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));

    assertThrows(
        ValidationException.class, () -> entregasService.cambiarEstado(id, requestSinFoto));
    assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstadoActual());
    verify(entregasRepository, never()).save(any());
    verifyNoInteractions(comunicadorEventos);
  }

  @Test
  void cambiarEstado_deberiaReportarNoRecepcion_cuandoEstadoEsNoRecibida() {
    Entrega entrega = EntregaMother.pendiente();
    entrega.iniciarRuta("Chofer");
    UUID id = entrega.getId();

    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.NO_RECIBIDA, "actor", "Motivo", false);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));
    when(entregasRepository.save(entrega)).thenReturn(entrega);

    EntregaResponseDTO resultado = entregasService.cambiarEstado(id, request);

    assertEquals(EstadoEntrega.NO_RECIBIDA, entrega.getEstadoActual());
    assertEquals(EstadoEntrega.NO_RECIBIDA, resultado.estadoActual());
    verify(entregasRepository).save(entrega);
    verify(comunicadorEventos).comunicarEntregaFallida(any(EntregaFallida.class));
  }

  @Test
  void cambiarEstado_deberiaRegresarAlDeposito_cuandoEstadoEsPendiente() {
    Entrega entrega = EntregaMother.pendiente();
    entrega.asignarRuta(UUID.randomUUID());
    entrega.iniciarRuta("Chofer");
    entrega.negarEntrega("actor", "Motivo", false);
    entrega.mandarARevision("Admin");
    UUID id = entrega.getId();

    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.PENDIENTE, "actor", null, null);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));
    when(entregasRepository.save(entrega)).thenReturn(entrega);

    EntregaResponseDTO resultado = entregasService.cambiarEstado(id, request);

    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstadoActual());
    assertEquals(EstadoEntrega.PENDIENTE, resultado.estadoActual());
    verify(entregasRepository).save(entrega);
  }

  @Test
  void cambiarEstado_deberiaLanzarExcepcion_cuandoEstadoNoAlcanzable() {
    Entrega entrega = EntregaMother.pendiente();
    UUID id = entrega.getId();
    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.EN_TRASLADO, "actor", null, null);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));

    assertThrows(ValidationException.class, () -> entregasService.cambiarEstado(id, request));
  }

  @Test
  void cambiarEstado_deberiaMandarARevision_cuandoEstadoEsRevision() {
    Entrega entrega = EntregaMother.pendiente();
    entrega.iniciarRuta("Chofer");
    entrega.negarEntrega("actor", "Motivo", false);
    UUID id = entrega.getId();

    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.REVISION, "Admin Carlos", null, null);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));
    when(entregasRepository.save(entrega)).thenReturn(entrega);

    EntregaResponseDTO resultado = entregasService.cambiarEstado(id, request);

    assertEquals(EstadoEntrega.REVISION, entrega.getEstadoActual());
    assertEquals(EstadoEntrega.REVISION, resultado.estadoActual());
    verify(entregasRepository).save(entrega);
  }

  @Test
  void cambiarEstado_deberiaRegistrarSolicitudDeTransicionEnAuditoria_cuandoRepositorioPresente() {
    var transicionRepo =
        mock(grupo5.logistica.models.repositories.ISolicitudesTransicionEntregaRepository.class);
    var serviceConAuditoria =
        new EntregasService(
            entregasRepository,
            rutasRepository,
            camionRepository,
            entregaMapper,
            comunicadorEventos,
            transicionRepo);

    Entrega entrega = EntregaMother.pendiente();
    entrega.iniciarRuta("Chofer");
    entrega.negarEntrega("actor", "Motivo", false);
    UUID id = entrega.getId();

    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.REVISION, "Admin Carlos", null, null);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));
    when(entregasRepository.save(entrega)).thenReturn(entrega);

    serviceConAuditoria.cambiarEstado(id, request);

    verify(transicionRepo).registrar(any());
  }

  @Test
  void cambiarEstado_deberiaLanzarExcepcion_cuandoIntentaPasarAEnTrasladoDirectamente() {
    Entrega entrega = EntregaMother.pendiente();
    UUID id = entrega.getId();
    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.EN_TRASLADO, "Chofer Jose", null, null);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));

    ValidationException ex =
        assertThrows(ValidationException.class, () -> entregasService.cambiarEstado(id, request));
    assertEquals(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA, ex.getError());
    verify(entregasRepository, never()).save(any());
  }
}
