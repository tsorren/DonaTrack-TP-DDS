package grupo5.common.repositories;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ValidationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrudRepositoryEnMemoriaTest {

  private TestRepository repository;

  private record TestAggregate(UUID id, String name) implements AggregateRoot {
    public TestAggregate(String name) {
      this(UUID.randomUUID(), name);
    }
  }

  private static class TestRepository extends CrudRepositoryEnMemoria<TestAggregate> {}

  @BeforeEach
  void setUp() {
    repository = new TestRepository();
  }

  @Test
  void testSaveYFindById() {
    TestAggregate aggregate = new TestAggregate("Test Name");
    repository.save(aggregate);

    Optional<TestAggregate> found = repository.findById(aggregate.id());
    assertTrue(found.isPresent());
    assertEquals("Test Name", found.get().name());
  }

  @Test
  void testFindAll() {
    TestAggregate aggregate1 = new TestAggregate("Test 1");
    TestAggregate aggregate2 = new TestAggregate("Test 2");
    repository.save(aggregate1);
    repository.save(aggregate2);

    List<TestAggregate> all = repository.findAll();
    assertEquals(2, all.size());
    assertTrue(all.stream().anyMatch(a -> a.name().equals("Test 1")));
    assertTrue(all.stream().anyMatch(a -> a.name().equals("Test 2")));
  }

  @Test
  void testSaveNuloLanzaExcepcion() {
    assertThrows(ValidationException.class, () -> repository.save(null));
  }

  @Test
  void testSaveConIdNuloLanzaExcepcion() {
    TestAggregate aggregateConIdNulo = new TestAggregate(null, "No Id");
    assertThrows(ValidationException.class, () -> repository.save(aggregateConIdNulo));
  }

  @Test
  void testFindByIdNuloLanzaExcepcion() {
    assertThrows(ValidationException.class, () -> repository.findById(null));
  }

  @Test
  void testExistsById() {
    TestAggregate aggregate = new TestAggregate("Test Name");
    assertFalse(repository.existsById(aggregate.id()));

    repository.save(aggregate);
    assertTrue(repository.existsById(aggregate.id()));
  }

  @Test
  void testExistsByIdNuloLanzaExcepcion() {
    assertThrows(ValidationException.class, () -> repository.existsById(null));
  }

  @Test
  void testDelete() {
    TestAggregate aggregate = new TestAggregate("Test Name");
    repository.save(aggregate);
    assertTrue(repository.existsById(aggregate.id()));

    repository.delete(aggregate);
    assertFalse(repository.existsById(aggregate.id()));
    assertEquals(0, repository.count());
  }

  @Test
  void testDeleteAll() {
    TestAggregate aggregate1 = new TestAggregate("Test 1");
    TestAggregate aggregate2 = new TestAggregate("Test 2");
    repository.save(aggregate1);
    repository.save(aggregate2);
    assertEquals(2, repository.count());

    repository.deleteAll();
    assertEquals(0, repository.count());
  }
}
