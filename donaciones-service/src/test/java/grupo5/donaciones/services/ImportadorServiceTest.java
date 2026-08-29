package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.personas.HumanaOutputDTO;
import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.ports.CargadorDonantes;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.services.impl.ImportadorService;
import grupo5.donaciones.services.impl.ValidadorPersonaDuplicada;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportadorServiceTest {

  private IArchivoDonantesRepository archivoRepository;
  private CargadorDonantes lectorCSV;
  private PersonaMapper personaMapper;
  private MedioDeContactoMapper medioDeContactoMapper;
  private ValidadorPersonaDuplicada validadorDuplicados;
  private IPersonasService personaService;
  private IDonantesService donantesService;
  private ImportadorService service;

  @BeforeEach
  void setUp() {
    archivoRepository = mock(IArchivoDonantesRepository.class);
    lectorCSV = mock(CargadorDonantes.class);
    personaMapper = mock(PersonaMapper.class);
    medioDeContactoMapper = new MedioDeContactoMapper();
    validadorDuplicados = mock(ValidadorPersonaDuplicada.class);
    personaService = mock(IPersonasService.class);
    donantesService = mock(IDonantesService.class);

    service =
        new ImportadorService(
            archivoRepository,
            lectorCSV,
            personaMapper,
            medioDeContactoMapper,
            validadorDuplicados,
            personaService,
            donantesService);
  }

  @Test
  void procesarImportacionAsincronica_conArchivoYFilasValidas_creaDonantesYFinaliza() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = new Archivo("/ruta/donantes.csv");
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));

    Map<String, String> fila =
        Map.of("TipoPersona", "HUMANA", "Nombre", "Juan", "Apellido", "Perez");
    when(lectorCSV.cargarDonantes(archivo.getPath())).thenReturn(List.of(fila));

    Humana personaMock = grupo5.donaciones.fixtures.PersonaMother.humanaValida();
    when(personaMapper.mapToPersona(fila)).thenReturn(personaMock);
    when(validadorDuplicados.buscarDuplicado(personaMock)).thenReturn(Optional.empty());

    PersonaOutputDTO outputDTO =
        new HumanaOutputDTO(
            TipoPersona.HUMANA,
            UUID.randomUUID(),
            TipoDocumento.DNI,
            "12345678",
            null,
            List.of(),
            "Juan",
            "Perez",
            null,
            null);
    when(personaService.crearPersona(any(PersonaInputDTO.class))).thenReturn(outputDTO);

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(EstadoArchivo.PROCESADO, archivo.getEstado());
    verify(personaService).crearPersona(any(PersonaInputDTO.class));
    verify(donantesService).crearDonante(any(DonanteInputDTO.class));
    verify(archivoRepository, times(2)).save(archivo);
  }

  @Test
  void procesarImportacionAsincronica_conPersonaDuplicada_actualizaPersona() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = new Archivo("/ruta/donantes.csv");
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));

    Map<String, String> fila =
        Map.of("TipoPersona", "HUMANA", "Nombre", "Juan", "Apellido", "Perez");
    when(lectorCSV.cargarDonantes(archivo.getPath())).thenReturn(List.of(fila));

    Humana personaMock = grupo5.donaciones.fixtures.PersonaMother.humanaValida();
    Humana personaExistente = grupo5.donaciones.fixtures.PersonaMother.humanaValida();

    when(personaMapper.mapToPersona(fila)).thenReturn(personaMock);
    when(validadorDuplicados.buscarDuplicado(personaMock))
        .thenReturn(Optional.of((Persona) personaExistente));

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(EstadoArchivo.PROCESADO, archivo.getEstado());
    verify(personaService).actualizarPersona(any(UUID.class), any(PersonaInputDTO.class));
    verify(archivoRepository, times(2)).save(archivo);
  }

  @Test
  void procesarImportacionAsincronica_cuandoFallaLector_marcaComoError() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = new Archivo("/ruta/donantes.csv");
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    when(lectorCSV.cargarDonantes(archivo.getPath()))
        .thenThrow(new RuntimeException("Error de I/O"));

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(EstadoArchivo.ERROR, archivo.getEstado());
    verify(archivoRepository, times(2)).save(archivo);
  }
}
