package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ValidadorPersonaDuplicada {

  private final IPersonasRepository personasRepository;

  public ValidadorPersonaDuplicada(IPersonasRepository personasRepository) {
    this.personasRepository = personasRepository;
  }

  /**
   * Busca una persona ya existente duplicada de {@code personaAImportar}. La decisión de qué
   * significa "ser duplicada" vive en el dominio (ver {@link Persona#esDuplicadaDe}); esta clase
   * solo trae los candidatos con consultas indexadas, por documento y por correo, en vez de cargar
   * todas las personas.
   *
   * @throws BusinessStateException {@code PERSONA_DUPLICADA_EN_CONFLICTO} si el correo pertenece a
   *     otra persona con un documento distinto
   */
  public Optional<Persona> buscarDuplicado(Persona personaAImportar) {
    // Por número y no por tipo y número: la fila puede no traer el tipo. El dominio decide si el
    // tipo es compatible; si dos personas distintas calzan (DNI y pasaporte con el mismo número),
    // no se adivina cuál es.
    List<Persona> porDocumento =
        personasRepository.findAllByNumeroDocumento(personaAImportar.getDocumento()).stream()
            .filter(personaAImportar::esDuplicadaDe)
            .toList();
    if (porDocumento.size() > 1) {
      throw new BusinessStateException(ErrorCatalog.PERSONA_DUPLICADA_EN_CONFLICTO);
    }
    if (porDocumento.size() == 1) {
      return Optional.of(porDocumento.getFirst());
    }

    List<Persona> porCorreo =
        personaAImportar.direccionesDeCorreo().stream()
            .flatMap(correo -> personasRepository.findByCorreo(correo).stream())
            .distinct()
            .toList();

    Optional<Persona> duplicada =
        porCorreo.stream().filter(personaAImportar::esDuplicadaDe).findFirst();
    if (duplicada.isEmpty() && porCorreo.stream().anyMatch(personaAImportar::entraEnConflictoCon)) {
      throw new BusinessStateException(ErrorCatalog.PERSONA_DUPLICADA_EN_CONFLICTO);
    }
    return duplicada;
  }
}
