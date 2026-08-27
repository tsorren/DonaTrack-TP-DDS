package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.incentivos.dto.DonanteRegistradoDTO;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.fixtures.IncentivosFixturesTest;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    RegistrarDonanteRequest request = IncentivosFixturesTest.registrarDonante(id);

    DonanteRegistradoDTO response = service.registrarDonante(request);

    assertNotNull(response);
    assertEquals(id, response.donanteId());
    assertTrue(repository.findById(id).isPresent());
  }

  @Test
  void registrarDonante_cuandoYaExiste_deberiaSerIdempotente() {
    UUID id = UUID.randomUUID();
    RegistrarDonanteRequest request = IncentivosFixturesTest.registrarDonante(id);

    service.registrarDonante(request);
    DonanteRegistradoDTO response = service.registrarDonante(request);

    assertNotNull(response);
    assertEquals(id, response.donanteId());
    assertEquals(1, repository.findAll().size());
  }

  @Test
  void modificarDonante_cuandoExiste_deberiaActualizarNombre() {
    UUID id = UUID.randomUUID();
    service.registrarDonante(
        IncentivosFixturesTest.registrarDonante(id, UUID.randomUUID(), "Inicial"));
    ModificarDonanteRequest request = IncentivosFixturesTest.modificarDonante("Modificado");

    service.modificarDonante(id, request);

    DonanteIncentivos guardado = repository.findById(id).orElseThrow();
    assertEquals(request.nombre(), guardado.getNombre());
  }

  @Test
  void modificarDonante_cuandoNoExiste_deberiaLanzarExcepcion() {
    UUID id = UUID.randomUUID();
    ModificarDonanteRequest request = IncentivosFixturesTest.modificarDonante("Nuevo");

    assertThrows(BusinessStateException.class, () -> service.modificarDonante(id, request));
  }

  @Test
  void obtenerDonante_cuandoExiste_deberiaRetornarEntidad() {
    UUID id = UUID.randomUUID();
    RegistrarDonanteRequest request = IncentivosFixturesTest.registrarDonante(id, id, "Test");
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
  void darDeBaja_cuandoExiste_deberiaEliminarDelRepositorio() {
    UUID id = UUID.randomUUID();
    service.registrarDonante(IncentivosFixturesTest.registrarDonante(id, id, "Test"));

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
    service.registrarDonante(IncentivosFixturesTest.registrarDonante(id1, id1, "Donante 1"));
    service.registrarDonante(IncentivosFixturesTest.registrarDonante(id2, id2, "Donante 2"));

    List<DonanteIncentivos> list = service.listarTodos();

    assertEquals(2, list.size());
  }
}
