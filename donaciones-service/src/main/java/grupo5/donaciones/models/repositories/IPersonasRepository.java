package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.listados.CriterioPersonas;
import grupo5.donaciones.models.repositories.listados.LecturaPorLote;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.util.List;
import java.util.Optional;

public interface IPersonasRepository extends CrudRepository<Persona>, LecturaPorLote<Persona> {

  /** Una página de personas, por nombre visible, con la búsqueda y los filtros (R11-01). */
  Tramo<Persona> buscar(CriterioPersonas criterio);

  Optional<Persona> findByDocumento(String documento);

  /** Personas con ese número de documento, con cualquier tipo (o sin tipo). */
  List<Persona> findAllByNumeroDocumento(String numero);

  /** Personas que tienen ese correo, sin distinguir mayúsculas ni espacios en los extremos. */
  List<Persona> findByCorreo(String direccionCorreo);
}
