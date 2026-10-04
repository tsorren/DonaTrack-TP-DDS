package grupo5.logistica.models.repositories;

import grupo5.logistica.models.entities.entregas.SolicitudTransicionEntrega;

public interface ISolicitudesTransicionEntregaRepository {
  void registrar(SolicitudTransicionEntrega solicitud);
}
