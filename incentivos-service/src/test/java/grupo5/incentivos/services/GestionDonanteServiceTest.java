package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.dto.DonanteRegistradoDTO;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.fixtures.IncentivosFixtures;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

class GestionDonanteServiceTest {

  private GestionDonanteService service;
  private DonanteIncentivosRepository repository;

  @BeforeEach
  void setUp() {
    repository = new DonanteIncentivosRepository();
    service = new GestionDonanteService(repository);
  }

  @Test
  void registrarDonante_cuandoEsNuevo_deberiaGuardarloYRetornarDTO() {
    UUID id = UUID.randomUUID();
    RegistrarDonanteRequest request = IncentivosFixtures.registrarDonante(id);

    DonanteRegistradoDTO response = service.registrarDonante(request);

    assertNotNull(response);
    assertEquals(id, response.donanteId());
    assertTrue(repository.findById(id).isPresent());
  }

  @Test
  void registrarDonante_cuandoYaExiste_deberiaSerIdempotente() {
    UUID id = UUID.randomUUID();
    RegistrarDonanteRequest request = IncentivosFixtures.registrarDonante(id);

    service.registrarDonante(request);
    DonanteRegistradoDTO response = service.registrarDonante(request);

    assertNotNull(response);
    assertEquals(id, response.donanteId());
    assertEquals(1, repository.findAll().size());
  }

  @Test
  void modificarDonante_cuandoExiste_deberiaActualizarNombre() {
    UUID id = UUID.randomUUID();
    service.registrarDonante(IncentivosFixtures.registrarDonante(id, UUID.randomUUID(), "Inicial"));
    ModificarDonanteRequest request = IncentivosFixtures.modificarDonante("Modificado");

    service.modificarDonante(id, request);

    DonanteIncentivos guardado = repository.findById(id).orElseThrow();
    assertEquals(request.nombre(), guardado.getNombre());
  }

  @Test
  void modificarDonante_cuandoNoExiste_deberiaLanzarExcepcion() {
    UUID id = UUID.randomUUID();
    ModificarDonanteRequest request = IncentivosFixtures.modificarDonante("Nuevo");

    assertThrows(BusinessStateException.class, () -> service.modificarDonante(id, request));
  }

  @Test
  void obtenerDonante_cuandoExiste_deberiaRetornarEntidad() {
    UUID id = UUID.randomUUID();
    RegistrarDonanteRequest request = IncentivosFixtures.registrarDonante(id, id, "Test");
    service.registrarDonante(request);

    DonanteIncentivos donante = service.obtenerDonante(id);

    assertNotNull(donante);
    assertEquals(id, donante.getId());
    assertEquals(request.nombre(), donante.getNombre());
  }

  @Test
  void obtenerDonante_cuandoNoExiste_deberiaLanzarExcepcion() {
    UUID id = UUID.randomUUID();
    assertThrows(BusinessStateException.class, () -> service.obtenerDonante(id));
  }

  @Test
  void buscarDonantePorPersonaId_cuandoExiste_deberiaRetornarDonante() {
    UUID id = UUID.randomUUID();
    UUID idPersona = UUID.randomUUID();
    service.registrarDonante(IncentivosFixtures.registrarDonante(id, idPersona, "Test"));

    var resultado = service.buscarDonantePorPersonaId(idPersona);

    assertTrue(resultado.isPresent());
    assertEquals(id, resultado.get().getId());
    assertEquals(idPersona, resultado.get().getIdPersona());
  }

  @Test
  void buscarDonantePorPersonaId_cuandoNoExiste_deberiaRetornarVacio() {
    var resultado = service.buscarDonantePorPersonaId(UUID.randomUUID());

    assertTrue(resultado.isEmpty());
  }

  @Test
  void buscarDonantePorPersonaId_cuandoIdPersonaEsNulo_deberiaRetornarVacio() {
    var resultado = service.buscarDonantePorPersonaId(null);

    assertTrue(resultado.isEmpty());
  }

  @Test
  void darDeBaja_cuandoExiste_deberiaEliminarDelRepositorio() {
    UUID id = UUID.randomUUID();
    service.registrarDonante(IncentivosFixtures.registrarDonante(id, id, "Test"));

    service.darDeBaja(id);

    assertFalse(repository.findById(id).isPresent());
  }

  @Test
  void darDeBaja_cuandoNoExiste_deberiaLanzarExcepcion() {
    UUID id = UUID.randomUUID();
    assertThrows(BusinessStateException.class, () -> service.darDeBaja(id));
  }

  @Test
  void listarTodos_deberiaRetornarTodosLosRegistrados() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    service.registrarDonante(IncentivosFixtures.registrarDonante(id1, id1, "Donante 1"));
    service.registrarDonante(IncentivosFixtures.registrarDonante(id2, id2, "Donante 2"));

    List<DonanteIncentivos> list = service.listarTodos();

    assertEquals(2, list.size());
  }

  @Test
  void modificarDonante_cuandoNoExiste_deberiaLanzarDonanteNoEncontrado() {
    UUID id = UUID.randomUUID();
    ModificarDonanteRequest request = IncentivosFixtures.modificarDonante("Nuevo");

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> service.modificarDonante(id, request));

    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO, ex.getError());
  }

  @Test
  void modificarDonante_cuandoRepositorioNoEncuentraDonante_noDeberiaGuardarNada() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    UUID id = UUID.randomUUID();
    when(repo.findById(id)).thenReturn(java.util.Optional.empty());
    GestionDonanteService servicio = new GestionDonanteService(repo);
    ModificarDonanteRequest request = IncentivosFixtures.modificarDonante("Nuevo");

    assertThrows(BusinessStateException.class, () -> servicio.modificarDonante(id, request));

    verify(repo, never()).save(any());
  }

  @Test
  void modificarDonante_cuandoExiste_deberiaCambiarElNombreEnElAgregadoYGuardarlo() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    UUID id = UUID.randomUUID();
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Inicial", java.util.List.of());
    when(repo.findById(id)).thenReturn(java.util.Optional.of(donante));
    GestionDonanteService servicio = new GestionDonanteService(repo);

    servicio.modificarDonante(id, IncentivosFixtures.modificarDonante("Nuevo"));

    assertEquals("Nuevo", donante.getNombre());
    verify(repo).save(donante);
  }

  @Test
  void darDeBaja_cuandoNoExiste_deberiaLanzarDonanteNoEncontrado() {
    UUID id = UUID.randomUUID();

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> service.darDeBaja(id));

    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO, ex.getError());
  }

  @Test
  void darDeBaja_cuandoYaSeDioDeBaja_deberiaLanzarDonanteNoEncontrado() {
    UUID id = UUID.randomUUID();
    service.registrarDonante(IncentivosFixtures.registrarDonante(id));
    service.darDeBaja(id);

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> service.darDeBaja(id));

    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO, ex.getError());
  }

  @Test
  void darDeBaja_cuandoExiste_deberiaDelegarEnEliminarPorIdSinCargarElAgregado() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    UUID id = UUID.randomUUID();
    when(repo.eliminarPorId(id)).thenReturn(true);
    GestionDonanteService servicio = new GestionDonanteService(repo);

    servicio.darDeBaja(id);

    verify(repo).eliminarPorId(id);
    verify(repo, never()).findById(any());
    verify(repo, never()).delete(any());
  }

  @Test
  void registrarDonante_cuandoYaExiste_noDeberiaVolverAGuardar() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    UUID id = UUID.randomUUID();
    when(repo.findById(id))
        .thenReturn(Optional.of(DonanteIncentivosMother.colaboradorSinMisiones(id)));
    GestionDonanteService servicio = new GestionDonanteService(repo);

    DonanteRegistradoDTO response =
        servicio.registrarDonante(IncentivosFixtures.registrarDonante(id));

    assertEquals(id, response.donanteId());
    verify(repo, never()).save(any());
  }

  @Test
  void registrarDonante_cuandoElSaveViolaIntegridadYElDonanteYaExiste_deberiaDevolverloSinFallar() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    UUID id = UUID.randomUUID();
    DonanteIncentivos ganador = DonanteIncentivosMother.colaboradorSinMisiones(id);
    when(repo.findById(id)).thenReturn(Optional.empty(), Optional.of(ganador));
    when(repo.save(any())).thenThrow(new DataIntegrityViolationException("pk duplicada"));
    GestionDonanteService servicio = new GestionDonanteService(repo);

    DonanteRegistradoDTO response =
        servicio.registrarDonante(IncentivosFixtures.registrarDonante(id));

    assertEquals(id, response.donanteId());
    assertEquals(ganador.getCategoria().name(), response.categoria());
  }

  @Test
  void
      registrarDonante_cuandoElSaveFallaPorConcurrenciaYElDonanteYaExiste_deberiaDevolverloSinFallar() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    UUID id = UUID.randomUUID();
    DonanteIncentivos ganador = DonanteIncentivosMother.colaboradorSinMisiones(id);
    when(repo.findById(id)).thenReturn(Optional.empty(), Optional.of(ganador));
    when(repo.save(any())).thenThrow(new OptimisticLockingFailureException("fila ya reemplazada"));
    GestionDonanteService servicio = new GestionDonanteService(repo);

    DonanteRegistradoDTO response =
        servicio.registrarDonante(IncentivosFixtures.registrarDonante(id));

    assertEquals(id, response.donanteId());
  }

  @Test
  void registrarDonante_cuandoElSaveViolaIntegridadYElDonanteNoExiste_deberiaRelanzarLaExcepcion() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    UUID id = UUID.randomUUID();
    DataIntegrityViolationException violacion =
        new DataIntegrityViolationException("persona_id duplicada");
    when(repo.findById(id)).thenReturn(Optional.empty());
    when(repo.save(any())).thenThrow(violacion);
    GestionDonanteService servicio = new GestionDonanteService(repo);
    RegistrarDonanteRequest request = IncentivosFixtures.registrarDonante(id);

    DataIntegrityViolationException lanzada =
        assertThrows(
            DataIntegrityViolationException.class, () -> servicio.registrarDonante(request));

    assertSame(violacion, lanzada);
  }
}
