package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
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
@Profile("!postgres")
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
    return storage.values().stream()
        .anyMatch(entrega -> Objects.equals(entrega.getIdDonacion(), idDonacion));
  }

  @Override
  public Optional<Entrega> findByIdDonacion(UUID idDonacion) {
    if (idDonacion == null) {
      return Optional.empty();
    }
    return storage.values().stream()
        .filter(entrega -> Objects.equals(entrega.getIdDonacion(), idDonacion))
        .findFirst();
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
}
