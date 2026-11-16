package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.CriterioSeguimiento;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.models.repositories.TramoDeEntregas;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres & !mongo")
public class EntregasRepository extends CrudRepositoryEnMemoria<Entrega>
    implements IEntregasRepository {

  private final List<UUID> ordenInsercion = new CopyOnWriteArrayList<>();
  private final IRutasRepository rutasRepository;

  public EntregasRepository() {
    this(null);
  }

  @Autowired
  public EntregasRepository(@Nullable IRutasRepository rutasRepository) {
    this.rutasRepository = rutasRepository;
  }

  @Override
  public Entrega save(Entrega aggregate) {
    Entrega guardada = super.save(aggregate);
    registrarOrdenInsercion(guardada.getId());
    return guardada;
  }

  @Override
  public List<Entrega> saveAll(List<Entrega> aggregates) {
    List<Entrega> guardadas = super.saveAll(aggregates);
    for (Entrega entrega : guardadas) {
      registrarOrdenInsercion(entrega.getId());
    }
    return guardadas;
  }

  @Override
  public void delete(Entrega aggregate) {
    super.delete(aggregate);
    ordenInsercion.remove(aggregate.getId());
  }

  @Override
  public void deleteAll() {
    super.deleteAll();
    ordenInsercion.clear();
  }

  @Override
  public List<Entrega> findByEstado(EstadoEntrega estado) {
    if (estado == null) {
      return List.of();
    }
    return storage.values().stream()
        .filter(entrega -> entrega.getEstadoActual() == estado)
        .sorted(Comparator.comparingInt(e -> indiceOrdenInsercion(e.getId())))
        .toList();
  }

  @Override
  public List<Entrega> findEntregadasParaMapa(Instant desde, Instant hasta, int limite) {
    return storage.values().stream()
        .filter(e -> esDelMapa(e, desde, hasta))
        .sorted(
            Comparator.comparing(Entrega::getHoraArribo, Comparator.reverseOrder())
                .thenComparing(Entrega::getId))
        .limit(Math.max(0, limite))
        .toList();
  }

  @Override
  public long contarEntregadasParaMapa(Instant desde, Instant hasta) {
    return storage.values().stream().filter(e -> esDelMapa(e, desde, hasta)).count();
  }

  private static boolean esDelMapa(Entrega entrega, Instant desde, Instant hasta) {
    if (desde == null
        || hasta == null
        || entrega.getEstadoActual() != EstadoEntrega.ENTREGADA
        || entrega.getHoraArribo() == null
        || entrega.getDestino() == null
        || !entrega.getDestino().tieneCoordenadas()) {
      return false;
    }
    Instant arribo = entrega.getHoraArribo().toInstant(ZoneOffset.UTC);
    return !arribo.isBefore(desde) && arribo.isBefore(hasta);
  }

  @Override
  public List<Entrega> findByRutaId(UUID rutaId) {
    if (rutaId == null) {
      return List.of();
    }
    List<UUID> ordenRuta =
        rutasRepository != null
            ? rutasRepository.findById(rutaId).map(Ruta::getEntregaIds).orElse(List.of())
            : List.of();

    return storage.values().stream()
        .filter(entrega -> Objects.equals(entrega.getIdRuta(), rutaId))
        .sorted(
            Comparator.comparingInt((Entrega e) -> indiceEnRuta(e.getId(), ordenRuta))
                .thenComparingInt(e -> indiceOrdenInsercion(e.getId())))
        .toList();
  }

  @Override
  public List<Entrega> findSinRuta() {
    return storage.values().stream()
        .filter(entrega -> entrega.getIdRuta() == null)
        .sorted(Comparator.comparingInt(e -> indiceOrdenInsercion(e.getId())))
        .toList();
  }

  @Override
  public boolean existsByIdDonacion(UUID idDonacion) {
    if (idDonacion == null) {
      return false;
    }
    return findByIdDonacion(idDonacion).isPresent();
  }

  @Override
  public Optional<Entrega> findByIdDonacion(UUID idDonacion) {
    if (idDonacion == null) {
      return Optional.empty();
    }
    return storage.values().stream()
        .filter(entrega -> Objects.equals(entrega.getIdDonacion(), idDonacion))
        .filter(entrega -> entrega.getEstadoActual() != EstadoEntrega.CANCELADA)
        .findFirst();
  }

  @Override
  public List<Entrega> findCanceladasByIdDonacion(UUID idDonacion) {
    return storage.values().stream()
        .filter(entrega -> Objects.equals(entrega.getIdDonacion(), idDonacion))
        .filter(entrega -> entrega.getEstadoActual() == EstadoEntrega.CANCELADA)
        .toList();
  }

  @Override
  public List<Entrega> findByIdBeneficiaria(UUID idBeneficiaria) {
    if (idBeneficiaria == null) {
      return List.of();
    }
    return storage.values().stream()
        .filter(entrega -> Objects.equals(entrega.getIdBeneficiaria(), idBeneficiaria))
        .sorted(Comparator.comparingInt(e -> indiceOrdenInsercion(e.getId())))
        .toList();
  }

  @Override
  public List<Entrega> findByDonanteId(UUID donanteId) {
    if (donanteId == null) {
      return List.of();
    }
    return storage.values().stream()
        .filter(entrega -> Objects.equals(entrega.getDonanteId(), donanteId))
        .sorted(Comparator.comparingInt(e -> indiceOrdenInsercion(e.getId())))
        .toList();
  }

  private void registrarOrdenInsercion(UUID id) {
    if (id != null && !ordenInsercion.contains(id)) {
      ordenInsercion.add(id);
    }
  }

  private int indiceOrdenInsercion(UUID id) {
    int index = ordenInsercion.indexOf(id);
    return index >= 0 ? index : Integer.MAX_VALUE;
  }

  private static int indiceEnRuta(UUID idEntrega, List<UUID> ordenRuta) {
    int index = ordenRuta.indexOf(idEntrega);
    return index >= 0 ? index : Integer.MAX_VALUE;
  }

  @Override
  public List<Entrega> findByIds(Collection<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return ids.stream().distinct().map(storage::get).filter(Objects::nonNull).toList();
  }

  @Override
  public TramoDeEntregas buscarSeguimiento(CriterioSeguimiento criterio) {
    return TramoDeEntregas.de(
        storage.values().stream()
            .filter(criterio::cumple)
            .sorted(CriterioSeguimiento.ORDEN)
            .toList(),
        criterio);
  }
}
