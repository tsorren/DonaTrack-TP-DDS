package grupo5.donaciones.models.repositories;

import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;


@Repository
public class DonacionesIndependientesRepository implements IDonacionesIndependientesRepository {

  private final List<DonacionIndependiente> almacen = new ArrayList<>();
  private final AtomicLong secuencia = new AtomicLong(1);

  @Override
  public DonacionIndependiente save(DonacionIndependiente donacion) {
    almacen.removeIf(d -> d == donacion); // reemplaza si ya existe (misma referencia)
    almacen.add(donacion);
    return donacion;
  }

  @Override
  public Optional<DonacionIndependiente> findById(Long id) {
    return almacen.stream()
        .filter(d -> System.identityHashCode(d) == id.intValue())
        .findFirst();
  }
}
