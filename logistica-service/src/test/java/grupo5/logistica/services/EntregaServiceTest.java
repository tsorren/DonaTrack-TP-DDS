package grupo5.logistica.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.entregas.*;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.*;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.services.impl.EntregasService;
import grupo5.logistica.services.mappers.EntregaMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

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
    entregaMapper = mock(EntregaMapper.class);
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

    CrearEntregaRequestDTO request = mock(CrearEntregaRequestDTO.class);

    Entrega entrega = mock(Entrega.class);

    EntregaResponseDTO response = mock(EntregaResponseDTO.class);

    when(entregaMapper.toEntity(request)).thenReturn(entrega);

    when(entregasRepository.save(entrega)).thenReturn(entrega);

    when(entregaMapper.toResponseDTO(entrega)).thenReturn(response);

    EntregaResponseDTO resultado = entregasService.crear(request);

    verify(entregasRepository).save(entrega);
    assertEquals(response, resultado);
  }

  @Test
  void crear_deberiaLanzarExcepcion_cuandoMapperDevuelveNull() {

    CrearEntregaRequestDTO request = mock(CrearEntregaRequestDTO.class);

    when(entregaMapper.toEntity(request)).thenReturn(null);

    assertThrows(ValidationException.class, () -> entregasService.crear(request));

    verify(entregasRepository, never()).save(any());
  }

  // ===================== listar() =====================

  @Test
  void listar_deberiaRetornarListaDeEntregas() {

    Entrega entrega = mock(Entrega.class);
    EntregaResponseDTO dto = mock(EntregaResponseDTO.class);

    when(entregasRepository.findAll()).thenReturn(List.of(entrega));

    when(entregaMapper.toResponseDTO(entrega)).thenReturn(dto);

    List<EntregaResponseDTO> resultado = entregasService.listar();

    assertEquals(1, resultado.size());
    assertEquals(dto, resultado.getFirst());
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

    UUID id = UUID.randomUUID();

    Entrega entrega = mock(Entrega.class);

    EntregaResponseDTO dto = mock(EntregaResponseDTO.class);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));

    when(entregaMapper.toResponseDTO(entrega)).thenReturn(dto);

    EntregaResponseDTO resultado = entregasService.obtenerPorId(id);

    assertEquals(dto, resultado);
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

    UUID id = UUID.randomUUID();

    Entrega entrega = mock(Entrega.class);

    AdjuntarFotoRecepcionRequestDTO request = new AdjuntarFotoRecepcionRequestDTO("foto.jpg");

    EntregaResponseDTO dto = mock(EntregaResponseDTO.class);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));

    when(entregasRepository.save(entrega)).thenReturn(entrega);

    when(entregaMapper.toResponseDTO(entrega)).thenReturn(dto);

    EntregaResponseDTO resultado = entregasService.adjuntarFotoRecepcion(id, request);

    verify(entrega).adjuntarFotoRecepcion("foto.jpg");
    verify(entregasRepository).save(entrega);
    assertEquals(dto, resultado);
  }

  // ===================== cambiarEstado() =====================

  @Test
  void cambiarEstado_deberiaConfirmarRecepcion_cuandoEstadoEsEntregada() {
    UUID id = UUID.randomUUID();
    Entrega entrega = mock(Entrega.class);
    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.ENTREGADA, "actor", null, null);
    EntregaResponseDTO dto = mock(EntregaResponseDTO.class);

    Ruta ruta = mock(Ruta.class);
    Camion camion = mock(Camion.class);
    EntregaConfirmada evento =
        new EntregaConfirmada(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));

    when(entrega.getIdRuta()).thenReturn(UUID.randomUUID());
    when(rutasRepository.findById(any())).thenReturn(Optional.of(ruta));
    when(ruta.getCamionId()).thenReturn(UUID.randomUUID());
    when(camionRepository.findById(any())).thenReturn(Optional.of(camion));
    when(entrega.getDomainEvents()).thenReturn(List.of(evento));

    when(entregasRepository.save(entrega)).thenReturn(entrega);
    when(entregaMapper.toResponseDTO(entrega)).thenReturn(dto);

    EntregaResponseDTO resultado = entregasService.cambiarEstado(id, request);

    // Como GestorDeEntregas se ejecuta estáticamente con el objeto de dominio que instanció el
    // Service,
    // el Gestor afectará a nuestro mock 'entrega', permitiéndonos verificar la llamada:
    verify(entrega).confirmarEntrega("actor");
    InOrder orden = inOrder(entregasRepository, comunicadorEventos, entrega);
    orden.verify(entregasRepository).save(entrega);
    orden.verify(comunicadorEventos).comunicarEntregaExitosa(evento, camion);
    orden.verify(entrega).clearDomainEvents();
    assertEquals(dto, resultado);
  }

  @Test
  void cambiarEstado_deberiaReportarNoRecepcion_cuandoEstadoEsNoRecibida() {
    UUID id = UUID.randomUUID();
    Entrega entrega = mock(Entrega.class);
    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.NO_RECIBIDA, "actor", "Motivo", false);
    EntregaResponseDTO dto = mock(EntregaResponseDTO.class);

    EntregaFallida evento =
        new EntregaFallida(UUID.randomUUID(), UUID.randomUUID(), "Motivo", false);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));
    when(entregasRepository.save(entrega)).thenReturn(entrega);
    when(entregaMapper.toResponseDTO(entrega)).thenReturn(dto);
    when(entrega.getDomainEvents()).thenReturn(List.of(evento));

    EntregaResponseDTO resultado = entregasService.cambiarEstado(id, request);

    verify(entrega).negarEntrega("actor", "Motivo", false);
    InOrder orden = inOrder(entregasRepository, comunicadorEventos, entrega);
    orden.verify(entregasRepository).save(entrega);
    orden.verify(comunicadorEventos).comunicarEntregaFallida(evento);
    orden.verify(entrega).clearDomainEvents();
    assertEquals(dto, resultado);
  }

  @Test
  void cambiarEstado_deberiaRegresarAlDeposito_cuandoEstadoEsPendiente() {
    UUID id = UUID.randomUUID();
    Entrega entrega = mock(Entrega.class);
    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.PENDIENTE, "actor", null, null);
    EntregaResponseDTO dto = mock(EntregaResponseDTO.class);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));
    when(entregasRepository.save(entrega)).thenReturn(entrega);
    when(entregaMapper.toResponseDTO(entrega)).thenReturn(dto);

    EntregaResponseDTO resultado = entregasService.cambiarEstado(id, request);

    verify(entrega).regresarAlDeposito("actor");
    assertEquals(dto, resultado);
  }

  @Test
  void cambiarEstado_deberiaLanzarExcepcion_cuandoEstadoNoAlcanzable() {
    UUID id = UUID.randomUUID();
    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.EN_TRASLADO, "actor", null, null);
    Entrega entrega = mock(Entrega.class);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));

    assertThrows(ValidationException.class, () -> entregasService.cambiarEstado(id, request));
  }
}
