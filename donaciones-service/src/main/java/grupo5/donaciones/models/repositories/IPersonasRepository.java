package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import java.util.List;
import java.util.Optional;

public interface IPersonasRepository extends CrudRepository<Persona> {
  Optional<Persona> findByDocumento(String documento);

  /** Busca por tipo y número de documento (único por la restricción D-13). */
  Optional<Persona> findByDocumento(TipoDocumento tipo, String numero);

  /** Personas con ese número de documento, con cualquier tipo (o sin tipo). */
  List<Persona> findAllByNumeroDocumento(String numero);

  /** Personas que tienen ese correo, sin distinguir mayúsculas ni espacios en los extremos. */
  List<Persona> findByCorreo(String direccionCorreo);
}
