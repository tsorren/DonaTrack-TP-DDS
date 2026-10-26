package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class PersonasRepositoryEnMemoria extends CrudRepositoryEnMemoria<Persona>
    implements IPersonasRepository {

  @Override
  public Optional<Persona> findByDocumento(String documento) {
    if (documento == null) {
      return Optional.empty();
    }
    return storage.values().stream().filter(p -> documento.equals(p.getDocumento())).findFirst();
  }

  @Override
  public List<Persona> findAllByNumeroDocumento(String numero) {
    if (numero == null || numero.isBlank()) {
      return List.of();
    }
    String buscado = numero.trim();
    return storage.values().stream().filter(p -> buscado.equals(p.getDocumento())).toList();
  }

  @Override
  public List<Persona> findByCorreo(String direccionCorreo) {
    if (direccionCorreo == null || direccionCorreo.isBlank()) {
      return List.of();
    }
    String buscado = direccionCorreo.trim();
    return storage.values().stream()
        .filter(
            p -> p.direccionesDeCorreo().stream().anyMatch(c -> c.trim().equalsIgnoreCase(buscado)))
        .toList();
  }
}
