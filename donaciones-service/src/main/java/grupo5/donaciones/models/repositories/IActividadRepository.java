package grupo5.donaciones.models.repositories;

import grupo5.donaciones.models.entities.actividad.Actividad;
import grupo5.donaciones.models.repositories.listados.CriterioActividad;
import grupo5.donaciones.models.repositories.listados.Tramo;

/** El registro de actividad de administración (R11-26): se agrega y se busca; no se edita. */
public interface IActividadRepository {

  void guardar(Actividad actividad);

  /** Una página con los filtros del criterio, de la más reciente a la más vieja. */
  Tramo<Actividad> buscar(CriterioActividad criterio);
}
