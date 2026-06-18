package grupo5.donaciones.models.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EntidadBeneficiariaRepositoryTest {
  private EntidadesBeneficiariasRepositoryEnMemoria repository;

  @BeforeEach
  void setUp() {
    repository = new EntidadesBeneficiariasRepositoryEnMemoria();
  }

  @Test
  void guardar_y_buscar_por_id() {
    Juridica juridica = mock(Juridica.class);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(juridica);

    repository.save(entidad);

    Optional<EntidadBeneficiaria> result = repository.findById(entidad.getId());

    assertTrue(result.isPresent());
    assertEquals(entidad.getId(), result.get().getId());
  }

  @Test
  void findAll_debe_retornar_todo() {
    EntidadBeneficiaria e1 = new EntidadBeneficiaria(mock(Juridica.class));
    EntidadBeneficiaria e2 = new EntidadBeneficiaria(mock(Juridica.class));

    repository.save(e1);
    repository.save(e2);

    List<EntidadBeneficiaria> result = repository.findAll();

    assertEquals(2, result.size());
  }
}
