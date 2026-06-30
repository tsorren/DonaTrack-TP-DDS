package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.personas.HumanaInputDTO;
import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.infrastructure.LectorCSVMejorado;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.services.IDonantesService;
import grupo5.donaciones.services.IImportadorService;
import grupo5.donaciones.services.IPersonasService;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ImportadorService implements IImportadorService {

  private final IArchivoDonantesRepository archivoRepository;
  private final LectorCSVMejorado lectorCSV;
  private final PersonaMapper personaMapper;
  private final ValidadorPersonaDuplicada validadorDuplicados;
  private final IPersonasService personaService;
  private final IDonantesService donantesService;

  public ImportadorService(
      IArchivoDonantesRepository archivoRepository,
      LectorCSVMejorado lectorCSV,
      PersonaMapper personaMapper,
      ValidadorPersonaDuplicada validadorDuplicados,
      IPersonasService personaService,
      IDonantesService donantesService) {
    this.archivoRepository = archivoRepository;
    this.lectorCSV = lectorCSV;
    this.personaMapper = personaMapper;
    this.validadorDuplicados = validadorDuplicados;
    this.personaService = personaService;
    this.donantesService = donantesService;
  }

  @Async
  public void procesarImportacionAsincronica(UUID archivoId) {
    Archivo archivo =
        archivoRepository
            .findById(archivoId)
            .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));

    try {
      archivo.marcarComoProcesando();
      archivoRepository.save(archivo);

      List<Map<String, String>> filas = lectorCSV.cargarDonantes(archivo.getPath());

      for (Map<String, String> fila : filas) {
        Persona personaClon = personaMapper.mapToPersona(fila);
        Optional<Persona> personaExistenteOpt = validadorDuplicados.buscarDuplicado(personaClon);

        if (personaExistenteOpt.isPresent()) {
          // Si la persona ya existe, actualizarla
          Persona personaExistente = personaExistenteOpt.get();
          UUID idExistente = personaExistente.getId();
          PersonaInputDTO dtoClon = transformarAPersonaInputDTO(personaClon);

          personaService.actualizarPersona(idExistente, dtoClon);

        } else {
          // Si la persona no existe, crear Persona y Donante
          PersonaInputDTO nuevaPersonaDto = transformarAPersonaInputDTO(personaClon);

          PersonaOutputDTO personaCreada = personaService.crearPersona(nuevaPersonaDto);

          DonanteInputDTO nuevoDonanteDto = new DonanteInputDTO(personaCreada.id());

          donantesService.crearDonante(nuevoDonanteDto);
        }
      }

      archivo.marcarComoProcesado();
      archivoRepository.save(archivo);

    } catch (Exception e) {
      archivo.marcarComoError();
      archivoRepository.save(archivo);
    }
  }

  private static PersonaInputDTO transformarAPersonaInputDTO(Persona persona) {
    String nombre = "";
    String apellido = "";
    Genero genero = null;
    LocalDate fechaNacimiento = null;

    if (persona instanceof Humana humana) {
      nombre = humana.getNombre();
      apellido = humana.getApellido();
      genero = humana.getGenero();
      fechaNacimiento = humana.getFechaNacimiento();
    }

    return new HumanaInputDTO(
        TipoPersona.HUMANA,
        persona.getTipoDocumento(),
        persona.getDocumento(),
        null,
        List.of(), // Medios de contacto
        nombre,
        apellido,
        genero,
        fechaNacimiento);
  }
}
