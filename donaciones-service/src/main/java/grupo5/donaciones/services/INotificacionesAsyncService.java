package grupo5.donaciones.services;

import grupo5.donaciones.dto.comunicaciones.EventoPersonaSincronizadaV1;

public interface INotificacionesAsyncService {
  void sincronizarPersona(EventoPersonaSincronizadaV1 evento);
}
