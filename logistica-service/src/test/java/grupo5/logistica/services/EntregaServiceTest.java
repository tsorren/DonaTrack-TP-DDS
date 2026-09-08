package grupo5.logistica.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.entregas.AdjuntarFotoRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaRequestDTO;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.services.impl.EntregasService;
import grupo5.logistica.services.mappers.DireccionMapper;
import grupo5.logistica.services.mappers.EntregaMapper;
import grupo5.logistica.testutils.EntregaMother;
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
  void crear_deberiaLanzarExcepcion_cuandoMapperDevuelveNull() {
    assertThrows(ValidationException.class, () -> entregasService.crear(null));
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

  // ===================== adjuntarFotoRecepcion() =====================

  @Test
  void adjuntarFotoRecepcion_deberiaGuardarFoto() {
    Entrega entrega = EntregaMother.pendiente();
    entrega.asignarRuta(UUID.randomUUID());
    entrega.iniciarRuta("Chofer");
    entrega.confirmarEntrega("Actor");
    UUID id = entrega.getId();

    AdjuntarFotoRecepcionRequestDTO request = new AdjuntarFotoRecepcionRequestDTO("foto.jpg");

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));
    when(entregasRepository.save(entrega)).thenReturn(entrega);

    EntregaResponseDTO resultado = entregasService.adjuntarFotoRecepcion(id, request);

    verify(entregasRepository).save(entrega);
    assertEquals("foto.jpg", resultado.fotoRecepcionUrl());
  }

  // ===================== cambiarEstado() =====================

  @Test
  void cambiarEstado_deberiaConfirmarRecepcion_cuandoEstadoEsEntregada() {
    UUID rutaId = UUID.randomUUID();
    UUID camionId = UUID.randomUUID();
    Entrega entrega = EntregaMother.pendiente();
    entrega.asignarRuta(rutaId);
    entrega.iniciarRuta("Chofer");
    UUID id = entrega.getId();

    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.ENTREGADA, "actor", null, null);

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
    verify(entregasRepository).save(entrega);
    verify(comunicadorEventos).comunicarEntregaExitosa(any(EntregaConfirmada.class), eq(camion));
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
