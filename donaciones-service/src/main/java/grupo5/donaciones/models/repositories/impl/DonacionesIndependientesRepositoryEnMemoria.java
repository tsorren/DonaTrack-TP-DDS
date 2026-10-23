package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class DonacionesIndependientesRepositoryEnMemoria
    extends CrudRepositoryEnMemoria<DonacionIndependiente>
    implements IDonacionesIndependientesRepository {

  @Override
  public List<DonacionIndependiente> findEnDeposito() {
    return findAll().stream()
        .filter(d -> d.getEstadoActual().getTipo() == TipoEstadoDonacion.EN_DEPOSITO)
        .toList();
  }

  @Override
  public int sumarCantidadAsignada(UUID necesidadId, UUID periodoId) {
    if (necesidadId == null) {
      return 0;
    }
    return storage.values().stream()
        .filter(d -> necesidadId.equals(d.getNecesidadId()))
        .filter(d -> Objects.equals(periodoId, d.getPeriodoId()))
        .mapToInt(DonacionIndependiente::getCantidad)
        .sum();
  }

  @Override
  public Map<UUID, Integer> contarPorEntidadRegistradasDesde(LocalDateTime desde) {
    Map<UUID, Integer> conteo = new HashMap<>();
    storage.values().stream()
        .filter(d -> d.getEntidadBeneficiariaId() != null)
        .filter(
            d ->
                desde == null
                    || (d.getFechaRegistro() != null && d.getFechaRegistro().isAfter(desde)))
        .forEach(d -> conteo.merge(d.getEntidadBeneficiariaId(), 1, Integer::sum));
    return conteo;
  }
}
