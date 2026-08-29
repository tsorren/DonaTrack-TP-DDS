package grupo5.donaciones.services.impl;

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
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.services.IDonantesService;
import grupo5.donaciones.services.IImportadorService;
import grupo5.donaciones.services.IPersonasService;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
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
  private static final Logger log = LoggerFactory.getLogger(ImportadorService.class);

  public ImportadorService(
      IArchivoDonantesRepository archivoRepository,
      CargadorDonantes lectorCSV,
      PersonaMapper personaMapper,
      MedioDeContactoMapper medioDeContactoMapper,
      ValidadorPersonaDuplicada validadorDuplicados,
      IPersonasService personaService,
      IDonantesService donantesService) {
    this.archivoRepository = archivoRepository;
    this.lectorCSV = lectorCSV;
    this.personaMapper = personaMapper;
    this.medioDeContactoMapper = medioDeContactoMapper;
    this.validadorDuplicados = validadorDuplicados;
    this.personaService = personaService;
    this.donantesService = donantesService;
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

      List<Map<String, String>> filasValidas = lectorCSV.cargarDonantes(archivo.getPath());

      int erroresDeNegocio = 0;

      for (Map<String, String> fila : filasValidas) {
        boolean exito = procesarFilaIndividual(fila);
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

  private boolean procesarFilaIndividual(Map<String, String> fila) {
    try {
      Persona personaClon = personaMapper.mapToPersona(fila);

      Optional<Persona> personaExistenteOpt = validadorDuplicados.buscarDuplicado(personaClon);

      PersonaInputDTO dto = transformarAPersonaInputDTO(personaClon, fila);

      if (personaExistenteOpt.isPresent()) {
        log.info("Actualizando persona existente con ID: {}", personaExistenteOpt.get().getId());
        personaService.actualizarPersona(personaExistenteOpt.get().getId(), dto);
      } else {
        log.info("Creando nueva persona y donante...");
        PersonaOutputDTO personaCreada = personaService.crearPersona(dto);
        donantesService.crearDonante(new DonanteInputDTO(personaCreada.id()));
      }
      return true;
    } catch (Exception e) {
      log.error("Error de negocio al procesar la fila {}: {}", fila, e.getMessage());
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
