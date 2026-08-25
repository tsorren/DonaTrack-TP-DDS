package grupo5.logistica.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.rutas.*;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.*;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.services.impl.RutasService;
import grupo5.logistica.services.mappers.RutaMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RutaServiceTest {

  private IRutasRepository rutasRepository;
  private IEntregasRepository entregasRepository;
  private ICamionRepository camionRepository;
  private IChoferesRepository choferesRepository;
  private RutaMapper rutaMapper;
  private ComunicadorEventosLogistica comunicadorEventos;

  private RutasService rutasService;

  @BeforeEach
  void setUp() {

    rutasRepository = mock(IRutasRepository.class);
    entregasRepository = mock(IEntregasRepository.class);
    camionRepository = mock(ICamionRepository.class);
    choferesRepository = mock(IChoferesRepository.class);
    rutaMapper = mock(RutaMapper.class);
    comunicadorEventos = mock(ComunicadorEventosLogistica.class);

    rutasService =
        new RutasService(
            rutasRepository,
            entregasRepository,
            camionRepository,
            choferesRepository,
            rutaMapper,
            comunicadorEventos);
  }

  // =====================================================
  // listar()
  // =====================================================

  @Test
  void listar_deberiaRetornarTodasLasRutas() {

    Ruta ruta = mock(Ruta.class);

    RutaResponseDTO dto = mock(RutaResponseDTO.class);

    when(rutasRepository.findAll()).thenReturn(List.of(ruta));

    when(rutaMapper.toResponseDTO(ruta)).thenReturn(dto);

    List<RutaResponseDTO> resultado = rutasService.listar();

    assertEquals(1, resultado.size());
    verify(rutaMapper).toResponseDTO(ruta);
  }

  @Test
  void listar_deberiaRetornarListaVaciaSiNoHayRutas() {

    when(rutasRepository.findAll()).thenReturn(List.of());

    List<RutaResponseDTO> resultado = rutasService.listar();

    assertTrue(resultado.isEmpty());
  }

  // =====================================================
  // obtenerPorId()
  // =====================================================

  @Test
  void obtenerPorId_deberiaRetornarRutaSiExiste() {

    UUID id = UUID.randomUUID();

    Ruta ruta = mock(Ruta.class);

    RutaResponseDTO dto = mock(RutaResponseDTO.class);

    when(rutasRepository.findById(id)).thenReturn(Optional.of(ruta));

    when(rutaMapper.toResponseDTO(ruta)).thenReturn(dto);

    RutaResponseDTO resultado = rutasService.obtenerPorId(id);

    assertEquals(dto, resultado);
  }

  @Test
  void obtenerPorId_deberiaLanzarExcepcionSiNoExiste() {

    UUID id = UUID.randomUUID();

    when(rutasRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> rutasService.obtenerPorId(id));
  }

  // =====================================================
  // agregarEntrega()
  // =====================================================

  @Test
  void agregarEntrega_deberiaAsignarEntregaALaRuta() {

    UUID rutaId = UUID.randomUUID();
    UUID entregaId = UUID.randomUUID();

    Ruta ruta = mock(Ruta.class);
    Entrega entrega = mock(Entrega.class);

    AgregarEntregaRutaRequestDTO dto = new AgregarEntregaRutaRequestDTO(entregaId);

    when(rutasRepository.findById(rutaId)).thenReturn(Optional.of(ruta));

    when(entregasRepository.findById(entregaId)).thenReturn(Optional.of(entrega));

    when(ruta.getId()).thenReturn(rutaId);
    when(ruta.getEstado()).thenReturn(EstadoRuta.PENDIENTE);
    when(ruta.getEntregaIds()).thenReturn(List.of());

    when(entrega.getId()).thenReturn(entregaId);
    when(entrega.getEstadoActual()).thenReturn(EstadoEntrega.PENDIENTE);

    RutaResponseDTO response = mock(RutaResponseDTO.class);

    when(rutaMapper.toResponseDTO(ruta)).thenReturn(response);

    RutaResponseDTO resultado = rutasService.agregarEntrega(rutaId, dto);

    verify(ruta).agregarEntrega(entregaId);
    verify(entrega).asignarRuta(rutaId);

    verify(rutasRepository).save(ruta);
    verify(entregasRepository).save(entrega);

    assertEquals(response, resultado);
  }

  @Test
  void agregarEntrega_deberiaFallarSiRequestEsNull() {
    UUID rutaId = UUID.randomUUID();

    assertThrows(ValidationException.class, () -> rutasService.agregarEntrega(rutaId, null));

    verifyNoInteractions(rutasRepository);
  }

  // =====================================================
  // cambiarEstado()
  // =====================================================

  @Test
  void cambiarEstado_deberiaIniciarRuta_cuandoEstadoEsEnTraslado() {
    UUID rutaId = UUID.randomUUID();
    UUID camionId = UUID.randomUUID();
    UUID choferId = UUID.randomUUID();
    UUID entregaId = UUID.randomUUID();

    Ruta ruta = mock(Ruta.class);
    Camion camion = mock(Camion.class);
    Chofer chofer = mock(Chofer.class);
    Entrega entrega = mock(Entrega.class);

    CambioEstadoRutaRequestDTO dto =
        new CambioEstadoRutaRequestDTO(EstadoRuta.EN_TRASLADO, choferId, "actor");

    when(rutasRepository.findById(rutaId)).thenReturn(Optional.of(ruta));
    when(ruta.getId()).thenReturn(rutaId);
    when(ruta.getCamionId()).thenReturn(camionId);
    when(ruta.getChoferId()).thenReturn(choferId);
    when(ruta.getEstado()).thenReturn(EstadoRuta.PENDIENTE);
    when(ruta.getEntregaIds()).thenReturn(List.of(entregaId));

    when(camionRepository.findById(camionId)).thenReturn(Optional.of(camion));
    when(choferesRepository.findById(choferId)).thenReturn(Optional.of(chofer));

    when(entregasRepository.findById(entregaId)).thenReturn(Optional.of(entrega));
    when(entrega.getId()).thenReturn(entregaId);
    when(entrega.getEstadoActual()).thenReturn(EstadoEntrega.PENDIENTE);

    when(camion.getId()).thenReturn(camionId);
    when(camion.estaDisponibleParaAsignar()).thenReturn(true);
    when(chofer.getId()).thenReturn(choferId);
    when(chofer.estaDisponibleParaAsignar()).thenReturn(true);

    when(rutaMapper.toResponseDTO(ruta)).thenReturn(mock(RutaResponseDTO.class));

    RutaResponseDTO resultado = rutasService.cambiarEstado(rutaId, dto);

    verify(ruta).iniciarRuta();
    verify(entrega).iniciarRuta("actor");
    verify(rutasRepository).save(ruta);
    verify(comunicadorEventos).comunicarRutaIniciada(eq(ruta), eq(camion), anyList());
    assertNotNull(resultado);
  }

  @Test
  void cambiarEstado_deberiaCompletarRuta_cuandoEstadoEsCompletada() {
    UUID rutaId = UUID.randomUUID();
    UUID camionId = UUID.randomUUID();
    UUID choferId = UUID.randomUUID();

    Ruta ruta = mock(Ruta.class);
    Camion camion = mock(Camion.class);
    Chofer chofer = mock(Chofer.class);

    CambioEstadoRutaRequestDTO dto =
        new CambioEstadoRutaRequestDTO(EstadoRuta.COMPLETADA, null, "actor");

    when(rutasRepository.findById(rutaId)).thenReturn(Optional.of(ruta));
    when(ruta.getCamionId()).thenReturn(camionId);
    when(ruta.getChoferId()).thenReturn(choferId);
    when(ruta.getId()).thenReturn(rutaId);
    when(ruta.getEstado()).thenReturn(EstadoRuta.EN_TRASLADO);

    when(camionRepository.findById(camionId)).thenReturn(Optional.of(camion));
    when(choferesRepository.findById(choferId)).thenReturn(Optional.of(chofer));

    when(camion.getRutaId()).thenReturn(rutaId);
    when(chofer.getRutaId()).thenReturn(rutaId);

    when(rutaMapper.toResponseDTO(ruta)).thenReturn(mock(RutaResponseDTO.class));

    RutaResponseDTO resultado = rutasService.cambiarEstado(rutaId, dto);

    verify(ruta).completarRuta();
    verify(rutasRepository).save(ruta);
    assertNotNull(resultado);
  }
}
