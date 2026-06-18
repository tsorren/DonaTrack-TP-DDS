package grupo5.donaciones.services;

import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import grupo5.donaciones.models.repositories.ArchivoDonantesRepository;
import org.springframework.stereotype.Service;

@Service
public class ArchivoDonantesService {

  private final ArchivoDonantesRepository archivoRepository;
  private final IImportadorService importadorService;

  public ArchivoDonantesService(
      ArchivoDonantesRepository archivoDonantesRepository, IImportadorService importadorService) {
    this.archivoRepository = archivoDonantesRepository;
    this.importadorService = importadorService;
  }

  public Archivo registrarArchivoInicial(String path) {
    Archivo archivo = new Archivo(path);
    archivo.setEstado(EstadoArchivo.PENDIENTE);
    return archivoRepository.save(archivo);
  }

  public void procesarArchivoMasivo(Archivo archivo) {
    importadorService.procesarImportacionAsincronica(archivo.getId());
  }
}
