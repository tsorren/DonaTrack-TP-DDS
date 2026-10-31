package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.config.AlmacenamientoProperties;
import grupo5.donaciones.dto.donantes.ArchivoDonantesSubido;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.ports.StoragePort;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.services.IArchivoDonantesService;
import grupo5.donaciones.services.IImportadorService;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ArchivoDonantesService implements IArchivoDonantesService {

  static final String TIPO_CONTENIDO_CSV = "text/csv";
  private static final String EXTENSION_CSV = ".csv";

  private final IArchivoDonantesRepository archivoRepository;
  private final IImportadorService importadorService;
  private final StoragePort storage;
  private final AlmacenamientoProperties almacenamiento;

  public ArchivoDonantesService(
      IArchivoDonantesRepository archivoDonantesRepository,
      IImportadorService importadorService,
      StoragePort storage,
      AlmacenamientoProperties almacenamiento) {
    this.archivoRepository = archivoDonantesRepository;
    this.importadorService = importadorService;
    this.storage = storage;
    this.almacenamiento = almacenamiento;
  }

  /**
   * Guarda el padrón en el almacenamiento con una clave propia (UUID, nunca el nombre del cliente)
   * y después registra el {@link Archivo} con esa clave. Si la base falla queda un objeto huérfano
   * en el bucket: es el costo aceptado de no tener una transacción entre ambos.
   */
  @Override
  public ArchivoOutputDTO cargarArchivoDonantes(ArchivoDonantesSubido subido) {
    validar(subido);
    String clave = UUID.randomUUID() + EXTENSION_CSV;
    storage.guardar(
        almacenamiento.bucketPadrones(),
        clave,
        subido.contenido(),
        subido.tamanio(),
        TIPO_CONTENIDO_CSV);

    Archivo archivo = new Archivo(clave);
    archivoRepository.save(archivo);

    importadorService.procesarImportacionAsincronica(archivo.getId());

    return toOutputDTO(archivo);
  }

  @Override
  public ArchivoOutputDTO obtenerPorId(UUID id) {
    Archivo archivo =
        archivoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    return toOutputDTO(archivo);
  }

  private void validar(ArchivoDonantesSubido subido) {
    String nombre = subido.nombreOriginal();
    if (subido.tamanio() <= 0
        || nombre == null
        || !nombre.toLowerCase(Locale.ROOT).endsWith(EXTENSION_CSV)) {
      throw new ValidationException(ErrorCatalog.ARCHIVO_DONANTES_INVALIDO);
    }
    if (subido.tamanio() > almacenamiento.csvTamanioMaximo().toBytes()) {
      throw new ValidationException(ErrorCatalog.ARCHIVO_DONANTES_TAMANIO_EXCEDIDO);
    }
  }

  private static ArchivoOutputDTO toOutputDTO(Archivo archivo) {
    return new ArchivoOutputDTO(archivo.getId(), archivo.getPath(), archivo.getEstado().toString());
  }
}
