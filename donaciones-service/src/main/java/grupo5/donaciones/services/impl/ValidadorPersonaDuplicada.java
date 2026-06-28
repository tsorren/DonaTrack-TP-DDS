package grupo5.donaciones.services.impl;

import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.services.CriterioDuplicado;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ValidadorPersonaDuplicada {

  private final List<CriterioDuplicado> criterios;

  public ValidadorPersonaDuplicada(List<CriterioDuplicado> criterios) {
    this.criterios = criterios;
  }

  public Optional<Persona> buscarDuplicado(Persona personaAImportar) {
    return criterios.stream()
        .map(criterio -> criterio.buscarCoincidencia(personaAImportar))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst(); // Retorna la primera coincidencia que encuentre
  }
}
