package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
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
  public List<DonacionIndependiente> findCancelablesPorNecesidad(UUID necesidadId) {
    return findAll().stream()
        .filter(d -> necesidadId != null && necesidadId.equals(d.getNecesidadId()))
        .filter(DonacionIndependiente::esCancelable)
        .toList();
  }

  @Override
  public Map<UUID, Integer> sumarCantidadAsignada(Map<UUID, UUID> periodoPorNecesidad) {
    Map<UUID, Integer> sumas = new HashMap<>();
    periodoPorNecesidad.forEach(
        (necesidadId, periodoId) ->
            sumas.put(necesidadId, sumarCantidadAsignada(necesidadId, periodoId)));
    return sumas;
  }

  @Override
  public int sumarCantidadAsignada(UUID necesidadId, UUID periodoId) {
    if (necesidadId == null) {
      return 0;
    }
    return storage.values().stream()
        .filter(d -> necesidadId.equals(d.getNecesidadId()))
        .filter(d -> Objects.equals(periodoId, d.getPeriodoId()))
        .mapToInt(DonacionIndependiente::getCantidadContada)
        .sum();
  }

  @Override
  public int sumarCantidadRecibida(UUID necesidadId, UUID periodoId) {
    if (necesidadId == null) {
      return 0;
    }
    return storage.values().stream()
        .filter(d -> necesidadId.equals(d.getNecesidadId()))
        .filter(d -> Objects.equals(periodoId, d.getPeriodoId()))
        .filter(
            d ->
                d.getEstadoActual() != null
                    && d.getEstadoActual().getTipo() == TipoEstadoDonacion.ENTREGADA)
        .mapToInt(DonacionIndependiente::getCantidad)
        .sum();
  }

  @Override
  public Map<UUID, Integer> sumarCantidadRecibida(Map<UUID, UUID> periodoPorNecesidad) {
    Map<UUID, Integer> sumas = new HashMap<>();
    periodoPorNecesidad.forEach(
        (necesidadId, periodoId) ->
            sumas.put(necesidadId, sumarCantidadRecibida(necesidadId, periodoId)));
    return sumas;
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

  @Override
  public long contarEnEstado(TipoEstadoDonacion estado) {
    return storage.values().stream().filter(d -> d.getEstadoActual().getTipo() == estado).count();
  }

  @Override
  public List<DonacionIndependiente> findDestacadasDesde(LocalDateTime desde, int limite) {
    return destacadasDesde(desde)
        .sorted(
            Comparator.comparing(DonacionIndependiente::getFechaEntrega)
                .reversed()
                .thenComparing(DonacionIndependiente::getId))
        .limit(limite)
        .toList();
  }

  @Override
  public long contarDestacadasDesde(LocalDateTime desde) {
    return destacadasDesde(desde).count();
  }

  private Stream<DonacionIndependiente> destacadasDesde(LocalDateTime desde) {
    return storage.values().stream()
        .filter(d -> d.getEstadoActual().getTipo() == TipoEstadoDonacion.ENTREGADA)
        .filter(d -> d.getFechaEntrega() != null && !d.getFechaEntrega().isBefore(desde));
  }
}
