package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.listados.CriterioDonaciones;
import grupo5.donaciones.models.repositories.listados.OrdenPorFecha;
import grupo5.donaciones.models.repositories.listados.TextoBuscado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class DonacionesRepositoryEnMemoria extends CrudRepositoryEnMemoria<Donacion>
    implements IDonacionesRepository {

  @Override
  public Set<UUID> idsDeDonantes() {
    return storage.values().stream().map(Donacion::getDonanteId).collect(Collectors.toSet());
  }

  @Override
  public Tramo<Donacion> buscar(CriterioDonaciones criterio) {
    Comparator<Donacion> porFecha = Comparator.comparing(Donacion::getFecha);
    if (criterio.orden() == OrdenPorFecha.RECIENTES) {
      porFecha = porFecha.reversed();
    }
    List<Donacion> filtradas =
        storage.values().stream()
            .filter(
                d -> criterio.donanteId() == null || criterio.donanteId().equals(d.getDonanteId()))
            .filter(
                d ->
                    criterio.desde() == null
                        || !d.getFecha().toLocalDate().isBefore(criterio.desde()))
            .filter(
                d ->
                    criterio.hasta() == null
                        || !d.getFecha().toLocalDate().isAfter(criterio.hasta()))
            .filter(d -> coincide(d, criterio.q()))
            .sorted(porFecha.thenComparing(Donacion::getId))
            .toList();
    return Tramo.de(filtradas, criterio.paginado());
  }

  /** La descripción de la donación o la de alguno de sus bienes contiene {@code q}. */
  private static boolean coincide(Donacion donacion, String q) {
    return q == null
        || TextoBuscado.contiene(donacion.getDescripcion(), q)
        || donacion.getItems().stream()
            .anyMatch(item -> TextoBuscado.contiene(item.bien().descripcion(), q));
  }
}
