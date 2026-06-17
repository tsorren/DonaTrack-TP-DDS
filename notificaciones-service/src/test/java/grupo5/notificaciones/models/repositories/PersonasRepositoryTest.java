package grupo5.notificaciones.models.repositories;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.notificaciones.models.entities.personas.Persona;
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
  void save_deberiaPersistirPersonaReplica() {
    Persona persona = new Persona();
    persona.setId(UUID.randomUUID());
    persona.setDenominacion("Replica Test");

    Persona saved = repository.save(persona);

    assertNotNull(saved);
    assertEquals(persona.getId(), saved.getId());
    assertTrue(repository.existsById(persona.getId()));
  }

  @Test
  void findById_deberiaRetornarPersonaSiExiste() {
    Persona persona = new Persona();
    UUID id = UUID.randomUUID();
    persona.setId(id);
    persona.setDenominacion("Replica 2");
    repository.save(persona);

    Optional<Persona> found = repository.findById(id);

    assertTrue(found.isPresent());
    assertEquals("Replica 2", found.get().getDenominacion());
  }

  @Test
  void delete_deberiaRemoverPersona() {
    Persona persona = new Persona();
    UUID id = UUID.randomUUID();
    persona.setId(id);
    repository.save(persona);

    repository.delete(persona);

    assertFalse(repository.existsById(id));
  }

  @Test
  void findAll_deberiaRetornarTodasLasReplicas() {
    Persona p1 = new Persona();
    p1.setId(UUID.randomUUID());
    Persona p2 = new Persona();
    p2.setId(UUID.randomUUID());

    repository.save(p1);
    repository.save(p2);

    List<Persona> all = repository.findAll();

    assertEquals(2, all.size());
  }
}
