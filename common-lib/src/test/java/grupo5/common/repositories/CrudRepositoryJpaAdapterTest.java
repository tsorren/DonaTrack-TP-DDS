package grupo5.common.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;

@ExtendWith(MockitoExtension.class)
class CrudRepositoryJpaAdapterTest {

  @Getter
  private static class TestAggregate implements AggregateRoot {
    private final UUID id;
    private final String data;

    TestAggregate(UUID id, String data) {
      this.id = id;
      this.data = data;
    }
  }

  @Getter
  private static class TestEntity {
    private final UUID id;
    private final String data;

    TestEntity(UUID id, String data) {
      this.id = id;
      this.data = data;
    }
  }

  private interface TestJpaRepository extends JpaRepository<TestEntity, UUID> {}

  private static class TestRepositoryAdapter
      extends CrudRepositoryJpaAdapter<TestAggregate, TestEntity, TestJpaRepository> {
    TestRepositoryAdapter(
        TestJpaRepository springDataRepo,
        Function<TestAggregate, TestEntity> toEntity,
        Function<TestEntity, TestAggregate> toDomain) {
      super(springDataRepo, toEntity, toDomain);
    }
  }

  @Mock private TestJpaRepository jpaRepository;

  private TestRepositoryAdapter adapter;

  private final Function<TestAggregate, TestEntity> toEntity =
      agg -> agg != null ? new TestEntity(agg.getId(), agg.getData()) : null;

  private final Function<TestEntity, TestAggregate> toDomain =
      ent -> ent != null ? new TestAggregate(ent.getId(), ent.getData()) : null;

  @BeforeEach
  void setUp() {
    adapter = new TestRepositoryAdapter(jpaRepository, toEntity, toDomain);
  }

  @Test
  void testSaveDeberiaMapearYPersistirEntidad() {
    UUID id = UUID.randomUUID();
    TestAggregate domain = new TestAggregate(id, "Valor");
    TestEntity entity = new TestEntity(id, "Valor");

    when(jpaRepository.save(any(TestEntity.class))).thenReturn(entity);

    TestAggregate resultado = adapter.save(domain);

    assertEquals(id, resultado.getId());
    assertEquals("Valor", resultado.getData());
    verify(jpaRepository).save(any(TestEntity.class));
  }

  @Test
  void testSaveAllDeberiaMapearYPersistirLista() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    TestAggregate domain1 = new TestAggregate(id1, "Val1");
    TestAggregate domain2 = new TestAggregate(id2, "Val2");
    TestEntity entity1 = new TestEntity(id1, "Val1");
    TestEntity entity2 = new TestEntity(id2, "Val2");

    when(jpaRepository.saveAll(any())).thenReturn(List.of(entity1, entity2));

    List<TestAggregate> resultado = adapter.saveAll(List.of(domain1, domain2));

    assertEquals(2, resultado.size());
    assertEquals("Val1", resultado.get(0).getData());
    assertEquals("Val2", resultado.get(1).getData());
    verify(jpaRepository).saveAll(any());
  }

  @Test
  void testFindByIdPresente() {
    UUID id = UUID.randomUUID();
    TestEntity entity = new TestEntity(id, "Encontrado");

    when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));

    Optional<TestAggregate> resultado = adapter.findById(id);

    assertTrue(resultado.isPresent());
    assertEquals(id, resultado.get().getId());
    assertEquals("Encontrado", resultado.get().getData());
    verify(jpaRepository).findById(id);
  }

  @Test
  void testFindByIdVacio() {
    UUID id = UUID.randomUUID();
    when(jpaRepository.findById(id)).thenReturn(Optional.empty());

    Optional<TestAggregate> resultado = adapter.findById(id);

    assertFalse(resultado.isPresent());
    verify(jpaRepository).findById(id);
  }

  @Test
  void testFindAll() {
    UUID id = UUID.randomUUID();
    TestEntity entity = new TestEntity(id, "Todos");

    when(jpaRepository.findAll()).thenReturn(List.of(entity));

    List<TestAggregate> resultado = adapter.findAll();

    assertEquals(1, resultado.size());
    assertEquals("Todos", resultado.get(0).getData());
    verify(jpaRepository).findAll();
  }

  @Test
  void testDelete() {
    UUID id = UUID.randomUUID();
    TestAggregate domain = new TestAggregate(id, "Para borrar");

    adapter.delete(domain);

    verify(jpaRepository).deleteById(id);
  }

  @Test
  void testExistsById() {
    UUID id = UUID.randomUUID();
    when(jpaRepository.existsById(id)).thenReturn(true);

    boolean existe = adapter.existsById(id);

    assertTrue(existe);
    verify(jpaRepository).existsById(id);
  }

  @Test
  void testCount() {
    when(jpaRepository.count()).thenReturn(5L);

    long cantidad = adapter.count();

    assertEquals(5L, cantidad);
    verify(jpaRepository).count();
  }

  @Test
  void testDeleteAll() {
    adapter.deleteAll();

    verify(jpaRepository).deleteAll();
  }
}
