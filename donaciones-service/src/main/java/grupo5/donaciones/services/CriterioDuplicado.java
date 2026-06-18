package grupo5.donaciones.services;

import grupo5.donaciones.models.entities.personas.Persona;
import java.util.Optional;

public interface CriterioDuplicado {
  Optional<Persona> buscarCoincidencia(Persona personaAImportar);
}
