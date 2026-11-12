package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.config.AlmacenamientoProperties;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donantes.ArchivoDonantesSubido;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.dto.donantes.FilaConErrorDTO;
import grupo5.donaciones.dto.donantes.FiltroArchivos;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.ports.StoragePort;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.models.repositories.listados.CriterioArchivos;
import grupo5.donaciones.models.repositories.listados.OrdenPorFecha;
import grupo5.donaciones.models.repositories.listados.Tramo;
import grupo5.donaciones.services.IArchivoDonantesService;
import grupo5.donaciones.services.IImportadorService;
import grupo5.donaciones.services.ParametrosListado;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
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
  private final Clock reloj;

  public ArchivoDonantesService(
      IArchivoDonantesRepository archivoDonantesRepository,
      IImportadorService importadorService,
      StoragePort storage,
      AlmacenamientoProperties almacenamiento,
      Clock reloj) {
    this.archivoRepository = archivoDonantesRepository;
    this.importadorService = importadorService;
    this.storage = storage;
    this.almacenamiento = almacenamiento;
    this.reloj = reloj;
  }

  /**
   * Guarda el padrón en el almacenamiento con una clave propia (UUID, nunca el nombre del cliente)
   * y después registra el {@link Archivo} con esa clave. Si la base falla queda un objeto huérfano
   * en el bucket: es el costo aceptado de no tener una transacción entre ambos. Guarda también el
   * nombre con que se subió y la hora de la subida (R11-20), del reloj del servicio.
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

    Archivo archivo = new Archivo(clave, subido.nombreOriginal(), LocalDateTime.now(reloj));
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

  @Override
  public Pagina<FilaConErrorDTO> obtenerFilasConError(UUID id, Integer page, Integer size) {
    EntidadBeneficiariaService.validarPaginado(page, size);
    Archivo archivo =
        archivoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    int pagina = page == null ? 0 : page;
    int tamanio = size == null ? EntidadBeneficiariaService.TAMANIO_PAGINA_POR_DEFECTO : size;
    return new Pagina<>(
        archivoRepository.buscarFilasConError(id, pagina, tamanio).stream()
            .map(f -> new FilaConErrorDTO(f.linea(), f.motivo().name(), f.codigo()))
            .toList(),
        archivo.getConError());
  }

  @Override
  public Pagina<ArchivoOutputDTO> listar(FiltroArchivos filtro, ParametrosListado listado) {
    LocalDate desde = filtro.desde();
    LocalDate hasta = filtro.hasta();
    if (desde != null && hasta != null && desde.isAfter(hasta)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    Tramo<Archivo> tramo =
        archivoRepository.buscar(
            new CriterioArchivos(
                filtro.estado() == null ? Set.of() : Set.copyOf(filtro.estado()),
                desde,
                hasta,
                listado.texto(),
                listado.orden(OrdenPorFecha.class, OrdenPorFecha.RECIENTES),
                listado.paginado()));
    return new Pagina<>(
        tramo.elementos().stream().map(ArchivoDonantesService::toOutputDTO).toList(),
        (int) tramo.total());
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
    return new ArchivoOutputDTO(
        archivo.getId(),
        archivo.getPath(),
        archivo.getEstado().toString(),
        archivo.getTotalFilas(),
        archivo.getProcesadas(),
        archivo.getCreadas(),
        archivo.getActualizadas(),
        archivo.getConError(),
        archivo.getUltimos().stream()
            .map(u -> new ArchivoOutputDTO.UltimoDTO(u.nombre(), u.resultado().name()))
            .toList(),
        archivo.getNombreOriginal(),
        archivo.getSubidoEn());
  }
}
