package grupo5.donaciones.services.impl;

import grupo5.donaciones.config.AlmacenamientoProperties;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoInputDTO;
import grupo5.donaciones.dto.personas.HumanaInputDTO;
import grupo5.donaciones.dto.personas.JuridicaInputDTO;
import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.ports.CargadorDonantes;
import grupo5.donaciones.models.ports.StoragePort;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.services.IDonantesService;
import grupo5.donaciones.services.IImportadorService;
import grupo5.donaciones.services.IPersonasService;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.io.IOException;
import java.io.InputStream;
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

      List<Map<String, String>> filasValidas = leerPadron(archivo);

      int erroresDeNegocio = 0;

      for (int i = 0; i < filasValidas.size(); i++) {
        // El número de fila es 1-based y no cuenta el encabezado
        boolean exito = procesarFilaIndividual(filasValidas.get(i), i + 1);
        if (!exito) {
          erroresDeNegocio++;
        }
      }

      archivo.finalizarProcesamiento(erroresDeNegocio);
      archivoRepository.save(archivo);

    } catch (Exception e) {
      log.error("Error fatal durante la importación del archivo {}: {}", archivoId, e.getMessage());
      archivo.marcarComoError();
      archivoRepository.save(archivo);
    }
  }

  /** El path del archivo es la clave del objeto en el bucket de padrones. */
  private List<Map<String, String>> leerPadron(Archivo archivo) throws IOException {
    try (InputStream contenido =
        storage.abrir(almacenamiento.bucketPadrones(), archivo.getPath())) {
      return lectorCSV.cargarDonantes(contenido);
    }
  }

  private boolean procesarFilaIndividual(Map<String, String> fila, int numeroFila) {
    try {
      Persona personaClon = personaMapper.mapToPersona(fila);

      Optional<Persona> personaExistenteOpt = validadorDuplicados.buscarDuplicado(personaClon);

      if (personaExistenteOpt.isPresent()) {
        UUID personaId = personaExistenteOpt.get().getId();
        if (donantesService.estaDadoDeBaja(personaId)) {
          log.warn(
              "La persona {} fue dada de baja como donante: no se la reactiva ni se actualiza",
              personaId);
          return false;
        }
        log.info("Actualizando persona existente con ID: {}", personaId);
        personaService.actualizarParcial(personaId, personaMapper.mapToActualizacionParcial(fila));
        // Recién con la fila válida y aplicada se registra el rol (y se publica donante.registrado)
        donantesService.registrarSiNoExiste(personaId);
      } else {
        log.info("Creando nueva persona y donante...");
        PersonaOutputDTO personaCreada =
            personaService.crearPersona(transformarAPersonaInputDTO(personaClon, fila));
        donantesService.crearDonante(new DonanteInputDTO(personaCreada.id()));
      }
      return true;
    } catch (Exception e) {
      // Solo el número de fila: la fila trae datos personales (nombre, documento, email)
      log.error("Error de negocio al procesar la fila {}: {}", numeroFila, e.getMessage());
      return false;
    }
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
