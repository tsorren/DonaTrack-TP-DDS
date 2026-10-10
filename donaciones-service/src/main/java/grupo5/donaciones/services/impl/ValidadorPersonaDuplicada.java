package grupo5.donaciones.services.impl;

import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ValidadorPersonaDuplicada {

  private final IPersonasRepository personasRepository;

  public ValidadorPersonaDuplicada(IPersonasRepository personasRepository) {
    this.personasRepository = personasRepository;
  }

  /**
   * Recorre el repositorio buscando una persona ya existente duplicada de {@code personaAImportar}.
   * La decisión de qué significa "ser duplicada" vive en el dominio (ver {@link
   * Persona#esDuplicadaDe}); esta clase solo resuelve el acceso a datos.
   */
  public Optional<Persona> buscarDuplicado(Persona personaAImportar) {
    return personasRepository.findAll().stream()
        .filter(personaAImportar::esDuplicadaDe)
        .findFirst();
  }
}
