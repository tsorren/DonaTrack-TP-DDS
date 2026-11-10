package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.DonaTrackException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.config.AlmacenamientoProperties;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoInputDTO;
import grupo5.donaciones.dto.personas.HumanaInputDTO;
import grupo5.donaciones.dto.personas.JuridicaInputDTO;
import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.FilaConError;
import grupo5.donaciones.models.entities.donantes.MotivoErrorFila;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.ports.CargadorDonantes;
import grupo5.donaciones.models.ports.CargadorDonantes.FilaPadron;
import grupo5.donaciones.models.ports.StoragePort;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.services.IDonantesService;
import grupo5.donaciones.services.IImportadorService;
import grupo5.donaciones.services.IPersonasService;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ImportadorService implements IImportadorService {

  /** Cada cuántas filas se guarda el avance (HB-14): ~40 escrituras para 20.000 filas. */
  public static final int TAMANIO_TANDA_PROGRESO = 500;

  private final IArchivoDonantesRepository archivoRepository;
  private final CargadorDonantes lectorCSV;
  private final PersonaMapper personaMapper;
  private final MedioDeContactoMapper medioDeContactoMapper;
  private final ValidadorPersonaDuplicada validadorDuplicados;
  private final IPersonasService personaService;
  private final IDonantesService donantesService;
  private final StoragePort storage;
  private final AlmacenamientoProperties almacenamiento;
  private static final Logger log = LoggerFactory.getLogger(ImportadorService.class);

  public ImportadorService(
      IArchivoDonantesRepository archivoRepository,
      CargadorDonantes lectorCSV,
      PersonaMapper personaMapper,
      MedioDeContactoMapper medioDeContactoMapper,
      ValidadorPersonaDuplicada validadorDuplicados,
      IPersonasService personaService,
      IDonantesService donantesService,
      StoragePort storage,
      AlmacenamientoProperties almacenamiento) {
    this.archivoRepository = archivoRepository;
    this.lectorCSV = lectorCSV;
    this.personaMapper = personaMapper;
    this.medioDeContactoMapper = medioDeContactoMapper;
    this.validadorDuplicados = validadorDuplicados;
    this.personaService = personaService;
    this.donantesService = donantesService;
    this.storage = storage;
    this.almacenamiento = almacenamiento;
  }

  @Async
  @Override
  public void procesarImportacionAsincronica(UUID archivoId) {
    Archivo archivo =
        archivoRepository
            .findById(archivoId)
            .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));

    try {
      archivo.marcarComoProcesando();
      archivoRepository.save(archivo);

      List<FilaPadron> filas = leerPadron(archivo);
      archivo.iniciarConteo(filas.size());
      archivoRepository.save(archivo);

      List<FilaConError> errores = new ArrayList<>();
      for (int i = 0; i < filas.size(); i++) {
        procesarFila(archivo, filas.get(i)).ifPresent(errores::add);
        if ((i + 1) % TAMANIO_TANDA_PROGRESO == 0) {
          guardarAvance(archivo, errores);
        }
      }

      archivo.finalizarProcesamiento();
      guardarAvance(archivo, errores);

    } catch (Exception e) {
      log.error("Error fatal durante la importación del archivo {}: {}", archivoId, e.getMessage());
      archivo.marcarComoError();
      archivoRepository.save(archivo);
    }
  }

  /**
   * Primero las filas con error y después los contadores, para que el total nunca las adelante. Si
   * falla la escritura de las filas, el archivo termina en ERROR y su {@code conError} puede contar
   * filas que no llegaron a la tabla.
   */
  private void guardarAvance(Archivo archivo, List<FilaConError> errores) {
    if (!errores.isEmpty()) {
      archivoRepository.guardarFilasConError(List.copyOf(errores));
      errores.clear();
    }
    archivoRepository.save(archivo);
  }

  /** El path del archivo es la clave del objeto en el bucket de padrones. */
  private List<FilaPadron> leerPadron(Archivo archivo) throws IOException {
    try (InputStream contenido =
        storage.abrir(almacenamiento.bucketPadrones(), archivo.getPath())) {
      return lectorCSV.cargarDonantes(contenido);
    }
  }

  /** Aplica una fila y la cuenta en el archivo; si no se importó, devuelve su error. */
  private Optional<FilaConError> procesarFila(Archivo archivo, FilaPadron fila) {
    if (!fila.formatoValido()) {
      return Optional.of(error(archivo, fila, MotivoErrorFila.FORMATO_INVALIDO, null));
    }
    try {
      Map<String, String> campos = fila.campos();
      Persona personaClon = personaMapper.mapToPersona(campos);

      Optional<Persona> personaExistenteOpt = validadorDuplicados.buscarDuplicado(personaClon);

      if (personaExistenteOpt.isPresent()) {
        UUID personaId = personaExistenteOpt.get().getId();
        if (donantesService.estaDadoDeBaja(personaId)) {
          log.warn(
              "La persona {} fue dada de baja como donante: no se la reactiva ni se actualiza",
              personaId);
          return Optional.of(error(archivo, fila, MotivoErrorFila.DONANTE_DADO_DE_BAJA, null));
        }
        log.info("Actualizando persona existente con ID: {}", personaId);
        personaService.actualizarParcial(
            personaId, personaMapper.mapToActualizacionParcial(campos));
        // Recién con la fila válida y aplicada se registra el rol (y se publica donante.registrado)
        donantesService.registrarSiNoExiste(personaId);
        archivo.registrarActualizada();
      } else {
        log.info("Creando nueva persona y donante...");
        PersonaOutputDTO personaCreada =
            personaService.crearPersona(transformarAPersonaInputDTO(personaClon, campos));
        donantesService.crearDonante(new DonanteInputDTO(personaCreada.id()));
        archivo.registrarCreada();
      }
      return Optional.empty();
    } catch (Exception e) {
      // Sin el mensaje de la excepción: puede citar datos de la fila (por ejemplo, la fecha de
      // nacimiento que no se pudo leer). Alcanzan la línea, el motivo y el código.
      String codigo = e instanceof DonaTrackException d ? d.getErrorCode() : null;
      MotivoErrorFila motivo = motivoDe(e);
      log.error(
          "Error de negocio al procesar la línea {}: {} ({}, {})",
          fila.linea(),
          motivo,
          e.getClass().getSimpleName(),
          codigo);
      return Optional.of(error(archivo, fila, motivo, codigo));
    }
  }

  private static FilaConError error(
      Archivo archivo, FilaPadron fila, MotivoErrorFila motivo, String codigo) {
    archivo.registrarError();
    return new FilaConError(archivo.getId(), fila.linea(), motivo, codigo);
  }

  private static MotivoErrorFila motivoDe(Exception e) {
    if (e instanceof DonaTrackException d
        && d.getError() == ErrorCatalog.PERSONA_DUPLICADA_EN_CONFLICTO) {
      return MotivoErrorFila.PERSONA_DUPLICADA_EN_CONFLICTO;
    }
    if (e instanceof ValidationException
        || e instanceof IllegalArgumentException
        || e instanceof DateTimeException) {
      return MotivoErrorFila.DATOS_INVALIDOS;
    }
    return MotivoErrorFila.ERROR_INESPERADO;
  }

  // lo que esta en null es porque no viene en el csv
  private PersonaInputDTO transformarAPersonaInputDTO(Persona persona, Map<String, String> fila) {
    String tipoPersonaStr =
        fila.getOrDefault("TIPO_PERSONA", fila.getOrDefault("TipoPersona", ""))
            .trim()
            .toUpperCase();

    List<MedioDeContactoInputDTO> mediosDeContacto =
        persona.getMediosDeContacto() != null
            ? persona.getMediosDeContacto().stream().map(medioDeContactoMapper::toInputDTO).toList()
            : List.of();

    if ("JURIDICA".equals(tipoPersonaStr) && persona instanceof Juridica juridica) {
      return new JuridicaInputDTO(
          TipoPersona.JURIDICA,
          juridica.getTipoDocumento(),
          juridica.getDocumento(),
          null,
          mediosDeContacto,
          juridica.getRazonSocial(),
          juridica.getTipo(),
          juridica.getRubro(),
          List.of());
    } else if (persona instanceof Humana humana) {
      return new HumanaInputDTO(
          TipoPersona.HUMANA,
          humana.getTipoDocumento(),
          humana.getDocumento(),
          null,
          mediosDeContacto,
          humana.getNombre(),
          humana.getApellido(),
          humana.getGenero(),
          humana.getFechaNacimiento());
    } else {
      throw new IllegalArgumentException(
          "El tipo de persona en el CSV no coincide con el objeto Persona procesado.");
    }
  }
}
