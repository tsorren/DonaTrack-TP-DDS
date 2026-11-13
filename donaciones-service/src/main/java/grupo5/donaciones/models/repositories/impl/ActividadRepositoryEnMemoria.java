package grupo5.donaciones.models.repositories.impl;

import grupo5.donaciones.models.entities.actividad.Actividad;
import grupo5.donaciones.models.repositories.IActividadRepository;
import grupo5.donaciones.models.repositories.listados.CriterioActividad;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/** Como la consulta del adaptador JPA: los mismos filtros, el mismo orden y el mismo tramo. */
@Repository
@Profile("!postgres")
public class ActividadRepositoryEnMemoria implements IActividadRepository {

  private final List<Actividad> filas = new CopyOnWriteArrayList<>();

  @Override
  public void guardar(Actividad actividad) {
    filas.add(actividad);
  }

  @Override
  public Tramo<Actividad> buscar(CriterioActividad criterio) {
    List<Actividad> filtradas =
        filas.stream()
            .filter(a -> criterio.autores().isEmpty() || criterio.autores().contains(a.autor()))
            .filter(a -> criterio.acciones().isEmpty() || criterio.acciones().contains(a.accion()))
            .filter(
                a ->
                    criterio.desde() == null
                        || !a.ocurrioEn().toLocalDate().isBefore(criterio.desde()))
            .filter(
                a ->
                    criterio.hasta() == null
                        || !a.ocurrioEn().toLocalDate().isAfter(criterio.hasta()))
            .sorted(
                Comparator.comparing(Actividad::ocurrioEn).reversed().thenComparing(Actividad::id))
            .toList();
    return Tramo.de(filtradas, criterio.paginado());
  }
}
