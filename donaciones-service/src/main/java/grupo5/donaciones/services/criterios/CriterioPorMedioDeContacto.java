package grupo5.donaciones.services.criterios;

import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.MedioDeContacto;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.Telefono;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.CriterioDuplicado;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CriterioPorMedioDeContacto implements CriterioDuplicado {

  private final IPersonasRepository personasRepository;

  public CriterioPorMedioDeContacto(IPersonasRepository personasRepository) {
    this.personasRepository = personasRepository;
  }

  @Override
  public Optional<Persona> buscarCoincidencia(Persona personaAImportar) {
    if (personaAImportar.getMediosDeContacto() == null
        || personaAImportar.getMediosDeContacto().isEmpty()) {
      return Optional.empty();
    }

    for (Persona personaGuardada : personasRepository.findAll()) {
      if (tienenMedioEnComun(personaAImportar, personaGuardada)) {
        return Optional.of(personaGuardada);
      }
    }
    return Optional.empty();
  }

  private boolean tienenMedioEnComun(Persona p1, Persona p2) {
    if (p1.getMediosDeContacto() == null || p2.getMediosDeContacto() == null) {
      return false;
    }
    for (MedioDeContacto medio1 : p1.getMediosDeContacto()) {
      for (MedioDeContacto medio2 : p2.getMediosDeContacto()) {
        if (hayCoincidenciaInteligente(medio1, medio2)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hayCoincidenciaInteligente(MedioDeContacto medio1, MedioDeContacto medio2) {
    if (medio1 == null || medio2 == null) return false;

    // Si ambos son Correos
    if (medio1 instanceof Correo correo1 && medio2 instanceof Correo correo2) {
      String val1 = correo1.getDireccionCorreo();
      String val2 = correo2.getDireccionCorreo();
      if (val1 == null || val2 == null) return false;

      return val1.trim().equalsIgnoreCase(val2.trim());
    }

    // Si ambos son Telefonos (al usar instanceof Telefono, también incluye automáticamente a
    // WhatsApp)
    if (medio1 instanceof Telefono tel1 && medio2 instanceof Telefono tel2) {
      String val1 = tel1.obtenerNumeroCompleto();
      String val2 = tel2.obtenerNumeroCompleto();
      if (val1 == null || val2 == null) return false;

      String limpio1 = val1.replaceAll("[^0-9]", "");
      String limpio2 = val2.replaceAll("[^0-9]", "");

      return !limpio1.isEmpty()
          && !limpio2.isEmpty()
          && (limpio1.endsWith(limpio2) || limpio2.endsWith(limpio1));
    }

    return false;
  }
}
