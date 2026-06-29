package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.donantes.ArchivoInputDTO;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.services.IImportadorService;
import org.springframework.stereotype.Service;

@Service
public class ArchivoDonantesService {

  private final IArchivoDonantesRepository archivoRepository;
  private final IImportadorService importadorService;

  public ArchivoDonantesService(
      IArchivoDonantesRepository archivoDonantesRepository, IImportadorService importadorService) {
    this.archivoRepository = archivoDonantesRepository;
    this.importadorService = importadorService;
  }

  public ArchivoOutputDTO cargarArchivoDonantes(ArchivoInputDTO input) {
    Archivo archivo = new Archivo(input.path());
    archivoRepository.save(archivo);

    importadorService.procesarImportacionAsincronica(archivo.getId());

    return new ArchivoOutputDTO(archivo.getId(), archivo.getPath(), archivo.getEstado().toString());
  }
}
