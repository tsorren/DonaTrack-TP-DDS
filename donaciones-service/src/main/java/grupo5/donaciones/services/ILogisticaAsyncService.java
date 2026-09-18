package grupo5.donaciones.services;

import grupo5.donaciones.dto.comunicaciones.NuevaEntregaRequest;

public interface ILogisticaAsyncService {
  void registrarEntregaPendiente(NuevaEntregaRequest request);
}
