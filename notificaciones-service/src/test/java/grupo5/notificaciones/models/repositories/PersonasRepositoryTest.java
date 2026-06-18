package grupo5.notificaciones.models.repositories;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import java.util.ArrayList;
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
    Persona persona =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Replica Test", TipoPersona.HUMANA);

    Persona saved = repository.save(persona);

    assertNotNull(saved);
    assertEquals(persona.getId(), saved.getId());
    assertTrue(repository.existsById(persona.getId()));
  }

  @Test
  void findById_deberiaRetornarPersonaSiExiste() {
    UUID id = UUID.randomUUID();
    Persona persona = new Persona(id, new ArrayList<>(), "Replica 2", TipoPersona.HUMANA);
    repository.save(persona);

    Optional<Persona> found = repository.findById(id);

    assertTrue(found.isPresent());
    assertEquals("Replica 2", found.get().getDenominacion());
  }

  @Test
  void delete_deberiaRemoverPersona() {
    UUID id = UUID.randomUUID();
    Persona persona = new Persona(id, new ArrayList<>(), "Replica Test", TipoPersona.HUMANA);
    repository.save(persona);

    repository.delete(persona);

    assertFalse(repository.existsById(id));
  }

  @Test
  void findAll_deberiaRetornarTodasLasReplicas() {
    Persona p1 = new Persona(UUID.randomUUID(), new ArrayList<>(), "P1", TipoPersona.HUMANA);
    Persona p2 = new Persona(UUID.randomUUID(), new ArrayList<>(), "P2", TipoPersona.HUMANA);

    repository.save(p1);
    repository.save(p2);

    List<Persona> all = repository.findAll();

    assertEquals(2, all.size());
  }
}
