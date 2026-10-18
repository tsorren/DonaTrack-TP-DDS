package grupo5.logistica.models.repositories.impl;

import grupo5.logistica.models.entities.entregas.SolicitudTransicionEntrega;
import grupo5.logistica.models.repositories.ISolicitudesTransicionEntregaRepository;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class SolicitudesTransicionEntregaRepository
    implements ISolicitudesTransicionEntregaRepository {

  private final List<SolicitudTransicionEntrega> storage = new CopyOnWriteArrayList<>();

  @Override
  public void registrar(SolicitudTransicionEntrega solicitud) {
    if (solicitud != null) {
      storage.add(solicitud);
    }
  }

  public List<SolicitudTransicionEntrega> findAll() {
    return List.copyOf(storage);
  }

  public void deleteAll() {
    storage.clear();
  }
}
