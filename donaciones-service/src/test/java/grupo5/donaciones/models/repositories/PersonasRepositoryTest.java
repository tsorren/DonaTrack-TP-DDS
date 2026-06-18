package grupo5.donaciones.models.repositories;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Persona;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PersonasRepositoryTest {

  private IPersonasRepository repository;

  @BeforeEach
  void setUp() {
    repository = new PersonasRepositoryEnMemoria();
  }

  @Test
  void save_deberiaPersistirPersona() {
    Humana humana = new Humana("Maria", "Lopez", LocalDate.of(1993, 4, 10));
    humana.setDocumento("22334455");

    Persona saved = repository.save(humana);

    assertNotNull(saved);
    assertEquals(humana.getId(), saved.getId());
    assertTrue(repository.existsById(humana.getId()));
  }

  @Test
  void findById_deberiaRetornarPersonaSiExiste() {
    Humana humana = new Humana("Pedro", "Gimenez", LocalDate.of(1985, 8, 20));
    repository.save(humana);

    Optional<Persona> found = repository.findById(humana.getId());

    assertTrue(found.isPresent());
    assertEquals("Pedro", ((Humana) found.get()).getNombre());
  }

  @Test
  void findById_deberiaRetornarVacioSiNoExiste() {
    Optional<Persona> found = repository.findById(UUID.randomUUID());
    assertTrue(found.isEmpty());
  }

  @Test
  void delete_deberiaRemoverPersona() {
    Humana humana = new Humana("Jose", "Sanz", LocalDate.of(1975, 12, 1));
    repository.save(humana);

    repository.delete(humana);

    assertFalse(repository.existsById(humana.getId()));
  }

  @Test
  void findAll_deberiaRetornarTodasLasPersonas() {
    Humana p1 = new Humana("A", "B", LocalDate.of(1990, 1, 1));
    Humana p2 = new Humana("C", "D", LocalDate.of(1991, 1, 1));

    repository.save(p1);
    repository.save(p2);

    List<Persona> all = repository.findAll();

    assertEquals(2, all.size());
  }
}
