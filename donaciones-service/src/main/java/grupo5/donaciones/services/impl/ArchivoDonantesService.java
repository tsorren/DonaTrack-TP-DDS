package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.donantes.ArchivoInputDTO;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.repositories.impl.ArchivoDonantesRepository;
import grupo5.donaciones.services.IImportadorService;
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

  public ArchivoOutputDTO cargarArchivoDonantes(ArchivoInputDTO input) {
    Archivo archivo = new Archivo(input.path());
    archivoRepository.save(archivo);

    importadorService.procesarImportacionAsincronica(archivo.getId());

    return new ArchivoOutputDTO(archivo.getId(), archivo.getPath(), archivo.getEstado().toString());
  }
}
