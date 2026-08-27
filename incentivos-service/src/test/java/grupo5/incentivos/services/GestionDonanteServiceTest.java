package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
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
  void registrarDonante_deberiaCrearPerfilConMisiones() {
    UUID id = UUID.randomUUID();
    service.registrarDonante(new RegistrarDonanteRequest(id, UUID.randomUUID(), "Test"));

    DonanteIncentivos donante = repository.findById(id).orElseThrow();

    assertNotNull(donante);
    assertEquals(CategoriaDonante.COLABORADOR, donante.getCategoria());
    assertFalse(donante.getMisiones().isEmpty());
  }

  @Test
  void registrarDonante_deberiaSerIdempotente() {
    UUID id = UUID.randomUUID();
    service.registrarDonante(new RegistrarDonanteRequest(id, UUID.randomUUID(), "Test"));
    service.registrarDonante(new RegistrarDonanteRequest(id, UUID.randomUUID(), "Test"));

    assertEquals(1, repository.findAll().size());
  }

  @Test
  void modificarDonante_deberiaActualizarNombre() {
    UUID id = UUID.randomUUID();
    service.registrarDonante(new RegistrarDonanteRequest(id, UUID.randomUUID(), "Inicial"));

    service.modificarDonante(id, new ModificarDonanteRequest("Modificado"));

    DonanteIncentivos donante = service.obtenerDonante(id);
    assertEquals("Modificado", donante.getNombre());
  }

  @Test
  void obtenerDonante_deberiaLanzarExcepcionSiNoExiste() {
    UUID uuid = new UUID(0L, 999L);
    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> service.obtenerDonante(uuid));
    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO, ex.getError());
  }

  @Test
  void darDeBaja_deberiaEliminarAlDonante() {
    UUID id = new UUID(0L, 40L);
    service.registrarDonante(new RegistrarDonanteRequest(id, id, "Test"));

    service.darDeBaja(id);

    assertFalse(repository.findById(id).isPresent());
  }

  @Test
  void darDeBaja_deberiaLanzarExcepcionSiDonanteNoExiste() {
    UUID uuid = new UUID(0L, 999L);
    assertThrows(BusinessStateException.class, () -> service.darDeBaja(uuid));
  }

  @Test
  void listarTodos_deberiaRetornarTodosLosDonantes() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    service.registrarDonante(new RegistrarDonanteRequest(id1, id1, "Donante 1"));
    service.registrarDonante(new RegistrarDonanteRequest(id2, id2, "Donante 2"));

    assertEquals(2, service.listarTodos().size());
  }
}
