package grupo5.donaciones.models.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.repositories.impl.EntidadesBeneficiariasRepositoryEnMemoria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntidadBeneficiariaRepositoryTest {
  private EntidadesBeneficiariasRepositoryEnMemoria repository;

  @BeforeEach
  void setUp() {
    repository = new EntidadesBeneficiariasRepositoryEnMemoria();
  }

  @Test
  void guardar_y_buscar_por_id() {
    UUID juridicaId = UUID.randomUUID();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(juridicaId);

    repository.save(entidad);

    Optional<EntidadBeneficiaria> result = repository.findById(entidad.getId());

    assertTrue(result.isPresent());
    assertEquals(entidad.getId(), result.get().getId());
  }

  @Test
  void findAll_debe_retornar_todo() {
    EntidadBeneficiaria e1 = new EntidadBeneficiaria(UUID.randomUUID());
    EntidadBeneficiaria e2 = new EntidadBeneficiaria(UUID.randomUUID());

    repository.save(e1);
    repository.save(e2);

    List<EntidadBeneficiaria> result = repository.findAll();

    assertEquals(2, result.size());
  }
}
