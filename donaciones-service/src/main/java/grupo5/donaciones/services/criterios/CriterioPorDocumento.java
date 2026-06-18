package grupo5.donaciones.services.criterios;

import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.CriterioDuplicado;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CriterioPorDocumento implements CriterioDuplicado {
  private final IPersonasRepository personasRepository;

  public CriterioPorDocumento(IPersonasRepository personasRepository) {
    this.personasRepository = personasRepository;
  }

  @Override
  public Optional<Persona> buscarCoincidencia(Persona personaAImportar) {
    if (personaAImportar.getDocumento() == null || personaAImportar.getDocumento().isBlank()) {
      return Optional.empty();
    }

    return personasRepository.findAll().stream()
        .filter(
            p ->
                p.getDocumento() != null
                    && p.getDocumento().equals(personaAImportar.getDocumento()))
        .findFirst();
  }
}
