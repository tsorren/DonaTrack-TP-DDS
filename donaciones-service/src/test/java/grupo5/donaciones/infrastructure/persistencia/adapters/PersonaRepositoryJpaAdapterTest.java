package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.infrastructure.persistencia.mapper.DireccionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.PersonasPersistenciaMapper;
import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.Telefono;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.privacidad.Anonimizable;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  PersonasPersistenciaMapper.class,
  DireccionesPersistenciaMapper.class,
  PersonaRepositoryJpaAdapter.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class PersonaRepositoryJpaAdapterTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private IPersonasRepository personas;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void limpiar() {
    personas.deleteAll();
    jdbc.update("DELETE FROM donaciones.direccion");
  }

  private int contar(String tabla) {
    return jdbc.queryForObject("SELECT count(*) FROM donaciones." + tabla, Integer.class);
  }

  private static Humana humana(String nombre, String documento) {
    Humana h =
        new Humana(nombre, "Prueba", LocalDate.of(1990, Month.JANUARY, 1), Genero.NO_BINARIO);
    h.actualizarDocumento(TipoDocumento.DNI, documento);
    return h;
  }

  private static Juridica juridica(Humana representante, String razonSocial, String cuit) {
    Juridica j = new Juridica(representante, razonSocial, TipoJuridico.ONG, "Social");
    j.actualizarDocumento(TipoDocumento.CUIT, cuit);
    return j;
  }

  @Test
  void guardarYLeer_humana_conservaSusCamposDireccionYMedios() {
    Humana original = PersonaMother.juanPerez();
    original.limpiarMediosDeContacto();
    original.agregarMedioDeContacto(PersonaMother.correoValido());
    original.agregarMedioDeContacto(PersonaMother.telefonoValido());

    personas.save(original);
    Humana leida =
        assertInstanceOf(Humana.class, personas.findById(original.getId()).orElseThrow());

    assertEquals("Juan", leida.getNombre());
    assertEquals("Pérez", leida.getApellido());
    assertEquals(Genero.HOMBRE, leida.getGenero());
    assertEquals(PersonaMother.FECHA_DEFECTO, leida.getFechaNacimiento());
    assertEquals(TipoDocumento.DNI, leida.getTipoDocumento());
    assertEquals("12345678", leida.getDocumento());
    assertEquals(PersonaMother.direccionValida(), leida.getDireccion());
    assertEquals(2, leida.getMediosDeContacto().size());
    Correo correo =
        assertInstanceOf(
            Correo.class,
            leida.getMediosDeContacto().stream()
                .filter(Correo.class::isInstance)
                .findFirst()
                .orElseThrow());
    assertEquals("contacto@ejemplo.com", correo.getDireccionCorreo());
    assertTrue(correo.getEsPredeterminado());
    Telefono telefono =
        assertInstanceOf(
            Telefono.class,
            leida.getMediosDeContacto().stream()
                .filter(Telefono.class::isInstance)
                .findFirst()
                .orElseThrow());
    assertEquals("+541144445555", telefono.obtenerNumeroCompleto());
    assertEquals(PersonaMother.telefonoValido().getTipo(), telefono.getTipo());
  }

  @Test
  void guardarYLeer_personaNueva_noAgregaOtroTelefonoVacioAlReconstruirla() {
    Humana original = humana("Ana", "11111111");
    int mediosAntes = original.getMediosDeContacto().size();

    personas.save(original);
    Persona leida = personas.findById(original.getId()).orElseThrow();

    assertEquals(mediosAntes, leida.getMediosDeContacto().size());
    assertEquals(mediosAntes, contar("medio_de_contacto"));
  }

  @Test
  void guardar_personaSinDocumentoNiDireccion_seLeeConEsosCamposNulos() {
    Humana original = new Humana("Ana", "Prueba", LocalDate.of(1990, Month.JANUARY, 1));

    personas.save(original);
    Persona leida = personas.findById(original.getId()).orElseThrow();

    assertNull(leida.getDocumento());
    assertNull(leida.getTipoDocumento());
    assertNull(leida.getDireccion());
  }

  @Test
  void guardar_dosPersonasConLaMismaDireccion_comparteUnaSolaFila() {
    Humana ana = humana("Ana", "11111111");
    Humana beto = humana("Beto", "22222222");
    ana.actualizarDireccion(PersonaMother.direccionValida());
    beto.actualizarDireccion(PersonaMother.direccionValida());

    personas.save(ana);
    personas.save(beto);

    assertEquals(1, contar("direccion"));
  }

  @Test
  void guardar_alReemplazarLosMedios_quedanSoloLosVigentes() {
    Humana original = humana("Ana", "11111111");
    original.agregarMedioDeContacto(PersonaMother.correoValido());
    personas.save(original);

    original.limpiarMediosDeContacto();
    original.agregarMedioDeContacto(PersonaMother.telefonoValido());
    personas.save(original);

    assertEquals(1, contar("medio_de_contacto"));
    assertEquals(1, contar("telefono"));
    assertEquals(0, contar("correo"));
  }

  @Test
  void guardar_dosPersonasConElMismoDocumento_fallaPorRestriccionUnica() {
    personas.save(humana("Ana", "11111111"));
    Humana mismoDocumento = humana("Beto", "11111111");

    assertThrows(DataIntegrityViolationException.class, () -> personas.save(mismoDocumento));
  }

  @Test
  void guardar_variasPersonasSinDocumento_noChocanEntreSi() {
    personas.save(new Humana("Ana", "Prueba", LocalDate.of(1990, Month.JANUARY, 1)));
    personas.save(new Humana("Beto", "Prueba", LocalDate.of(1990, Month.JANUARY, 1)));

    assertEquals(2, personas.count());
  }

  @Test
  void findByDocumento_devuelveLaPersonaQueTieneEseDocumento() {
    Humana ana = humana("Ana", "11111111");
    personas.save(ana);
    personas.save(humana("Beto", "22222222"));

    assertEquals(ana.getId(), personas.findByDocumento("11111111").orElseThrow().getId());
    assertTrue(personas.findByDocumento("99999999").isEmpty());
    assertTrue(personas.findByDocumento(null).isEmpty());
  }

  @Test
  void guardarYLeer_juridica_conservaSusCamposYRepresentantes() {
    Humana primero = humana("Ana", "11111111");
    Humana segundo = humana("Beto", "22222222");
    Juridica original = juridica(primero, "Fundación Prueba", "30-11111111-1");
    original.agregarRepresentante(segundo);
    original.actualizarDireccion(PersonaMother.direccionValida());

    personas.save(original);
    Juridica leida =
        assertInstanceOf(Juridica.class, personas.findById(original.getId()).orElseThrow());

    assertEquals("Fundación Prueba", leida.getRazonSocial());
    assertEquals(TipoJuridico.ONG, leida.getTipo());
    assertEquals("Social", leida.getRubro());
    assertEquals(TipoDocumento.CUIT, leida.getTipoDocumento());
    assertEquals(PersonaMother.direccionValida(), leida.getDireccion());
    List<UUID> ids = leida.getRepresentantes().stream().map(Humana::getId).toList();
    assertEquals(2, ids.size());
    assertTrue(ids.contains(primero.getId()));
    assertTrue(ids.contains(segundo.getId()));
    assertEquals(3, personas.count());
  }

  @Test
  void guardar_representanteAntesQueLaJuridica_funciona() {
    Humana representante = humana("Ana", "11111111");
    Juridica juridica = juridica(representante, "Fundación Prueba", "30-11111111-1");

    // Mismo orden que PersonasService: primero los representantes, después la jurídica
    personas.save(representante);
    personas.save(juridica);

    Juridica leida =
        assertInstanceOf(Juridica.class, personas.findById(juridica.getId()).orElseThrow());
    assertEquals(1, leida.getRepresentantes().size());
    assertEquals(2, personas.count());
  }

  @Test
  void guardar_alQuitarUnRepresentante_laHumanaNoSeBorra() {
    Humana primero = humana("Ana", "11111111");
    Humana segundo = humana("Beto", "22222222");
    Juridica juridica = juridica(primero, "Fundación Prueba", "30-11111111-1");
    juridica.agregarRepresentante(segundo);
    personas.save(juridica);

    juridica.quitarRepresentante(segundo);
    personas.save(juridica);

    Juridica leida =
        assertInstanceOf(Juridica.class, personas.findById(juridica.getId()).orElseThrow());
    assertEquals(
        List.of(primero.getId()), leida.getRepresentantes().stream().map(Humana::getId).toList());
    assertTrue(personas.findById(segundo.getId()).isPresent());
  }

  @Test
  void guardar_humanaSola_noLeQuitaSuRepresentacion() {
    Humana representante = humana("Ana", "11111111");
    Juridica juridica = juridica(representante, "Fundación Prueba", "30-11111111-1");
    personas.save(juridica);

    representante.actualizar(
        "Ana María", "Prueba", LocalDate.of(1990, Month.JANUARY, 1), Genero.MUJER);
    personas.save(representante);

    Juridica leida =
        assertInstanceOf(Juridica.class, personas.findById(juridica.getId()).orElseThrow());
    assertEquals(1, leida.getRepresentantes().size());
    assertEquals("Ana María", leida.getRepresentantes().getFirst().getNombre());
  }

  @Test
  void guardar_humanaQueYaRepresentaAOtraJuridica_fallaYNoLaMueve() {
    Humana representante = humana("Ana", "11111111");
    Juridica primera = juridica(representante, "Primera", "30-11111111-1");
    personas.save(primera);
    Juridica segunda = juridica(representante, "Segunda", "30-22222222-2");

    assertThrows(BusinessStateException.class, () -> personas.save(segunda));

    Juridica leida =
        assertInstanceOf(Juridica.class, personas.findById(primera.getId()).orElseThrow());
    assertEquals(1, leida.getRepresentantes().size());
    assertFalse(personas.findById(segunda.getId()).isPresent());
  }

  @Test
  void guardar_personaAnonimizada_persisteLosValoresAnonimizados() {
    Humana humana = PersonaMother.juanPerez();
    humana.agregarMedioDeContacto(PersonaMother.correoValido());
    personas.save(humana);

    humana.anonimizar();
    personas.save(humana);
    Humana leida = assertInstanceOf(Humana.class, personas.findById(humana.getId()).orElseThrow());

    assertEquals(Anonimizable.VALOR_STRING, leida.getNombre());
    assertNull(leida.getDocumento());
    assertEquals(Anonimizable.VALOR_STRING, leida.getDireccion().calle());
    assertTrue(
        leida.getMediosDeContacto().stream()
            .filter(Correo.class::isInstance)
            .map(Correo.class::cast)
            .allMatch(c -> Anonimizable.VALOR_STRING.equals(c.getDireccionCorreo())));
  }

  @Test
  void eliminar_persona_borraSusMediosDeContacto() {
    Humana humana = humana("Ana", "11111111");
    humana.agregarMedioDeContacto(PersonaMother.correoValido());
    personas.save(humana);

    personas.delete(humana);

    assertEquals(0, contar("persona"));
    assertEquals(0, contar("medio_de_contacto"));
  }

  @Test
  void eliminar_unRepresentante_dejaALaJuridicaConLosRestantes() {
    Humana primero = humana("Ana", "11111111");
    Humana segundo = humana("Beto", "22222222");
    Juridica juridica = juridica(primero, "Fundación Prueba", "30-11111111-1");
    juridica.agregarRepresentante(segundo);
    personas.save(juridica);

    personas.delete(segundo);

    Juridica leida =
        assertInstanceOf(Juridica.class, personas.findById(juridica.getId()).orElseThrow());
    assertEquals(1, leida.getRepresentantes().size());
  }
}
