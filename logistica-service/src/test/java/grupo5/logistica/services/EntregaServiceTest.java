package grupo5.logistica.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.entregas.*;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.NoRecepcion;
import grupo5.logistica.models.entities.entregas.RegresoDeposito;
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

class EntregaServiceTest {

  private IEntregasRepository entregasRepository;
  private EntregaMapper entregaMapper;
  private ComunicadorEventosLogistica comunicadorEventos;
  private EntregasService entregasService;

  @BeforeEach
  void setUp() {
    entregasRepository = mock(IEntregasRepository.class);
    IRutasRepository rutasRepository = mock(IRutasRepository.class);
    ICamionRepository camionRepository = mock(ICamionRepository.class);
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

  // ===================== reportarNoRecepcion() =====================

  @Test
  void reportarNoRecepcion_deberiaGuardarYPublicarEvento() {

    UUID id = UUID.randomUUID();

    Entrega entrega = mock(Entrega.class);

    ReportarNoRecepcionRequestDTO request =
        new ReportarNoRecepcionRequestDTO("actor", "No estaba", true);

    EntregaResponseDTO dto = mock(EntregaResponseDTO.class);
    NoRecepcion solicitud = new NoRecepcion(entrega, "actor", "No estaba", true);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));

    when(entregasRepository.save(entrega)).thenReturn(entrega);

    when(entregaMapper.toSolicitud(entrega, request)).thenReturn(solicitud);
    when(entregaMapper.toResponseDTO(entrega)).thenReturn(dto);

    EntregaResponseDTO resultado = entregasService.reportarNoRecepcion(id, request);

    verify(entrega).negarEntrega("actor");
    verify(comunicadorEventos).comunicarEntregaFallida(solicitud);

    assertEquals(dto, resultado);
  }

  // ===================== regresarAlDeposito() =====================

  @Test
  void regresarAlDeposito_deberiaGuardarEntrega() {

    UUID id = UUID.randomUUID();

    Entrega entrega = mock(Entrega.class);

    RegresarAlDepositoRequestDTO request = new RegresarAlDepositoRequestDTO("chofer");

    EntregaResponseDTO dto = mock(EntregaResponseDTO.class);
    RegresoDeposito solicitud = new RegresoDeposito(entrega, "chofer");

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));

    when(entregasRepository.save(entrega)).thenReturn(entrega);

    when(entregaMapper.toSolicitud(entrega, request)).thenReturn(solicitud);
    when(entregaMapper.toResponseDTO(entrega)).thenReturn(dto);

    EntregaResponseDTO resultado = entregasService.regresarAlDeposito(id, request);

    verify(entrega).regresarAlDeposito("chofer");
    verify(entregasRepository).save(entrega);

    assertEquals(dto, resultado);
  }

  // ===================== obtenerHistorial() =====================

  @Test
  void obtenerHistorial_deberiaRetornarHistorialDeEntrega() {

    UUID id = UUID.randomUUID();

    Entrega entrega = mock(Entrega.class);

    when(entregasRepository.findById(id)).thenReturn(Optional.of(entrega));

    when(entrega.getHistorialEstado()).thenReturn(List.of());

    List<CambioEstadoEntregaResponseDTO> resultado = entregasService.obtenerHistorial(id);

    assertTrue(resultado.isEmpty());
  }

  // ===================== validaciones null =====================

  @Test
  void confirmarRecepcion_deberiaLanzarExcepcion_siRequestEsNull() {

    UUID id = UUID.randomUUID();

    assertThrows(ValidationException.class, () -> entregasService.confirmarRecepcion(id, null));
  }

  @Test
  void adjuntarFotoRecepcion_deberiaLanzarExcepcion_siRequestEsNull() {

    UUID id = UUID.randomUUID();

    assertThrows(ValidationException.class, () -> entregasService.adjuntarFotoRecepcion(id, null));
  }

  @Test
  void reportarNoRecepcion_deberiaLanzarExcepcion_siRequestEsNull() {

    UUID id = UUID.randomUUID();

    assertThrows(ValidationException.class, () -> entregasService.reportarNoRecepcion(id, null));
  }

  @Test
  void regresarAlDeposito_deberiaLanzarExcepcion_siRequestEsNull() {

    UUID id = UUID.randomUUID();

    assertThrows(ValidationException.class, () -> entregasService.regresarAlDeposito(id, null));
  }
}
