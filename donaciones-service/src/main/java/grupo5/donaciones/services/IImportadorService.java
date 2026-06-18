package grupo5.donaciones.services;

import java.util.UUID;

public interface IImportadorService {
  void procesarImportacionAsincronica(UUID archivoId);
}
