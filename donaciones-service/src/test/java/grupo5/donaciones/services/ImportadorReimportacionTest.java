package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.MedioDeContacto;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.ports.CargadorDonantes;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.models.repositories.impl.PersonasRepositoryEnMemoria;
import grupo5.donaciones.services.impl.ImportadorService;
import grupo5.donaciones.services.impl.NotificacionesAsyncService;
import grupo5.donaciones.services.impl.PersonasService;
import grupo5.donaciones.services.impl.ValidadorPersonaDuplicada;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reimportar un CSV sobre personas que ya existen no debe pisar los datos que el CSV no trae. Usa
 * el repositorio, el mapper y los servicios reales; solo se simulan los bordes (archivo, lector y
 * notificaciones).
 */
class ImportadorReimportacionTest {

  private static final String DOCUMENTO_JURIDICA = "30-87654321-9";
  private static final String DOCUMENTO_HUMANA = "12345678";

  private PersonasRepositoryEnMemoria personasRepository;
  private IArchivoDonantesRepository archivoRepository;
  private CargadorDonantes lectorCSV;
  private IDonantesService donantesService;
  private ImportadorService importador;

  @BeforeEach
  void setUp() {
    personasRepository = new PersonasRepositoryEnMemoria();
    archivoRepository = mock(IArchivoDonantesRepository.class);
    lectorCSV = mock(CargadorDonantes.class);
    donantesService = mock(IDonantesService.class);

    MedioDeContactoMapper medioMapper = new MedioDeContactoMapper();
    PersonaMapper personaMapper = new PersonaMapper(new DireccionMapper(), medioMapper);
    PersonasService personasService =
        new PersonasService(
            personasRepository, personaMapper, mock(NotificacionesAsyncService.class));

    importador =
        new ImportadorService(
            archivoRepository,
            lectorCSV,
            personaMapper,
            medioMapper,
            new ValidadorPersonaDuplicada(personasRepository),
            personasService,
            donantesService);
  }

  @Test
  void reimportarJuridicaSinEsosDatos_conservaDireccionRepresentantesTipoYRubro() {
    Juridica ong = PersonaMother.fundacionEsperanza();
    ong.agregarMedioDeContacto(PersonaMother.correoValido());
    guardar(ong);
    Direccion direccionOriginal = ong.getDireccion();
    int mediosAntes = ong.getMediosDeContacto().size();
    UUID representanteOriginal = ong.getRepresentantes().getFirst().getId();

    Archivo archivo = importar(Map.of("TipoPersona", "JURIDICA", "DOCUMENTO", DOCUMENTO_JURIDICA));

    Juridica despues = (Juridica) personasRepository.findById(ong.getId()).orElseThrow();
    assertEquals(EstadoArchivo.PROCESADO, archivo.getEstado());
    assertEquals(TipoJuridico.ONG, despues.getTipo());
    assertEquals("Fundación Esperanza", despues.getRazonSocial());
    assertEquals("Social", despues.getRubro());
    assertEquals(direccionOriginal, despues.getDireccion());
    assertEquals(1, despues.getRepresentantes().size());
    assertEquals(representanteOriginal, despues.getRepresentantes().getFirst().getId());
    assertEquals(TipoDocumento.CUIT, despues.getTipoDocumento());
    assertEquals(mediosAntes, despues.getMediosDeContacto().size());
    verify(donantesService, never()).crearDonante(any(DonanteInputDTO.class));
  }

  @Test
  void reimportarJuridicaConEmailNuevo_agregaElMedioSinBorrarLosExistentes() {
    Juridica ong = PersonaMother.fundacionEsperanza();
    ong.agregarMedioDeContacto(PersonaMother.correoValido());
    ong.agregarMedioDeContacto(PersonaMother.telefonoValido());
    guardar(ong);
    int mediosAntes = ong.getMediosDeContacto().size();

    importar(
        Map.of(
            "TipoPersona", "JURIDICA",
            "DOCUMENTO", DOCUMENTO_JURIDICA,
            "EMAIL", "nuevo@fundacion.org"));

    Juridica despues = (Juridica) personasRepository.findById(ong.getId()).orElseThrow();
    assertEquals(mediosAntes + 1, despues.getMediosDeContacto().size());
    assertTrue(contieneCorreo(despues.getMediosDeContacto(), "contacto@ejemplo.com"));
    assertTrue(contieneCorreo(despues.getMediosDeContacto(), "nuevo@fundacion.org"));
  }

  @Test
  void reimportarHumanaSinEsosDatos_conservaNombreFechaGeneroDireccionYMedios() {
    Humana juan = PersonaMother.juanPerez();
    juan.agregarMedioDeContacto(PersonaMother.correoValido());
    juan.agregarMedioDeContacto(PersonaMother.telefonoValido());
    guardar(juan);
    Direccion direccionOriginal = juan.getDireccion();
    LocalDate fechaOriginal = juan.getFechaNacimiento();
    int mediosAntes = juan.getMediosDeContacto().size();

    Archivo archivo =
        importar(
            Map.of(
                "TipoPersona", "HUMANA", "TIPO_DOCUMENTO", "DNI", "DOCUMENTO", DOCUMENTO_HUMANA));

    Humana despues = (Humana) personasRepository.findById(juan.getId()).orElseThrow();
    assertEquals(EstadoArchivo.PROCESADO, archivo.getEstado());
    assertEquals("Juan", despues.getNombre());
    assertEquals("Pérez", despues.getApellido());
    assertEquals(fechaOriginal, despues.getFechaNacimiento());
    assertEquals(juan.getGenero(), despues.getGenero());
    assertEquals(direccionOriginal, despues.getDireccion());
    assertEquals(mediosAntes, despues.getMediosDeContacto().size());
  }

  @Test
  void reimportarHumanaConDatosNuevos_aplicaSoloLosQueElCsvTrae() {
    Humana juan = PersonaMother.juanPerez();
    guardar(juan);
    Direccion direccionOriginal = juan.getDireccion();

    importar(
        Map.of(
            "TipoPersona", "HUMANA",
            "TIPO_DOCUMENTO", "DNI",
            "DOCUMENTO", DOCUMENTO_HUMANA,
            "Nombre", "Juan Carlos",
            "FECHA_NACIMIENTO", "1985-03-20"));

    Humana despues = (Humana) personasRepository.findById(juan.getId()).orElseThrow();
    assertEquals("Juan Carlos", despues.getNombre());
    assertEquals(LocalDate.of(1985, 3, 20), despues.getFechaNacimiento());
    assertEquals("Pérez", despues.getApellido());
    assertEquals(direccionOriginal, despues.getDireccion());
  }

  @Test
  void reimportarConTipoDePersonaDistintoAlExistente_cuentaErrorYNoModificaNada() {
    Humana juan = PersonaMother.juanPerez();
    guardar(juan);

    Archivo archivo =
        importar(
            Map.of("TipoPersona", "JURIDICA", "DOCUMENTO", DOCUMENTO_HUMANA, "RAZON_SOCIAL", "X"));

    Humana despues = (Humana) personasRepository.findById(juan.getId()).orElseThrow();
    assertEquals(EstadoArchivo.PROCESADO_CON_ERRORES, archivo.getEstado());
    assertEquals("Juan", despues.getNombre());
  }

  private void guardar(Juridica juridica) {
    juridica.getRepresentantes().forEach(personasRepository::save);
    personasRepository.save(juridica);
  }

  private void guardar(Humana humana) {
    personasRepository.save(humana);
  }

  private Archivo importar(Map<String, String> fila) {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = new Archivo("/ruta/donantes.csv");
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    when(lectorCSV.cargarDonantes(archivo.getPath())).thenReturn(List.of(fila));

    importador.procesarImportacionAsincronica(archivoId);
    return archivo;
  }

  private static boolean contieneCorreo(List<MedioDeContacto> medios, String direccion) {
    return medios.stream()
        .anyMatch(m -> m instanceof Correo c && direccion.equals(c.getDireccionCorreo()));
  }
}
