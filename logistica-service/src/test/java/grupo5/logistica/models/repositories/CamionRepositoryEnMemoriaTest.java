package grupo5.logistica.models.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.repositories.impl.CamionRepository;
import grupo5.logistica.testutils.CamionMother;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CamionRepositoryEnMemoriaTest {

  @Test
  void findAll_incluyeLosDeshabilitadosYFindActivosLosExcluye() {
    CamionRepository repository = new CamionRepository();
    Camion disponible = CamionMother.disponible();
    Camion deshabilitado = CamionMother.disponible();
    deshabilitado.deshabilitar();
    repository.saveAll(List.of(disponible, deshabilitado));

    Set<EstadoCamion> todos =
        repository.findAll().stream().map(Camion::getEstado).collect(Collectors.toSet());

    assertEquals(Set.of(EstadoCamion.DISPONIBLE, EstadoCamion.DESHABILITADO), todos);
    assertEquals(
        List.of(disponible.getId()), repository.findActivos().stream().map(Camion::getId).toList());
  }
}
