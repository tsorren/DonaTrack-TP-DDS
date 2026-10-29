package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.infrastructure.persistencia.mapper.DireccionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesIndependientesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.ItemsNormalizadosPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.NecesidadesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.PropuestasPersistenciaMapper;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.GestorPropuestasDeAsignacion;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IAsignacionesRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.services.IDonacionesEventPublisher;
import grupo5.donaciones.services.impl.AcumuladosDeAsignacion;
import grupo5.donaciones.services.impl.PropuestaDeAsignacionService;
import grupo5.donaciones.services.logistica.ILogisticaBroker;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.EjecucionAsignacionMapper;
import grupo5.donaciones.services.mappers.PropuestaMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Aprobar una propuesta contra Postgres real y con el proxy transaccional de Spring: las donaciones
 * asignadas, el pedido al broker y el estado de la propuesta se confirman juntos o no se confirma
 * nada.
 */
@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  DireccionesPersistenciaMapper.class,
  DonacionesPersistenciaMapper.class,
  DonacionRepositoryJpaAdapter.class,
  ItemsNormalizadosPersistenciaMapper.class,
  ItemDonacionNormalizadoRepositoryJpaAdapter.class,
  NecesidadesPersistenciaMapper.class,
  NecesidadRepositoryJpaAdapter.class,
  DonacionesIndependientesPersistenciaMapper.class,
  DonacionIndependienteRepositoryJpaAdapter.class,
  PropuestasPersistenciaMapper.class,
  PropuestaRepositoryJpaAdapter.class,
  AcumuladosDeAsignacion.class,
  PropuestaDeAsignacionService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class AprobacionPropuestaTransaccionalJpaTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private PropuestaDeAsignacionService service;
  @Autowired private IPropuestasRepository propuestas;
  @Autowired private IDonacionesIndependientesRepository independientes;
  @Autowired private IDonacionesRepository donaciones;
  @Autowired private IItemDonacionNormalizadoRepository itemsNormalizados;
  @Autowired private INecesidadesRepository necesidades;
  @Autowired private JdbcTemplate jdbc;

  // Colaboradores que no son persistencia: lo único que importa de ellos es si el broker falla
  @MockitoBean private ILogisticaBroker logisticaBroker;
  @MockitoBean private IDonacionesEventPublisher donacionesEventPublisher;
  @MockitoBean private IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  @MockitoBean private IPersonasRepository personasRepository;
  @MockitoBean private IDonantesRepository donantesRepository;
  @MockitoBean private IAsignacionesRepository asignacionRepository;
  @MockitoBean private GestorPropuestasDeAsignacion gestorPropuestas;
  @MockitoBean private PropuestaMapper propuestaMapper;
  @MockitoBean private EjecucionAsignacionMapper ejecucionMapper;
  @MockitoBean private DireccionMapper direccionMapper;

  private NecesidadExtraordinaria necesidad;
  private Propuesta propuesta;

  @BeforeEach
  void preparar() {
    propuestas.deleteAll();
    independientes.deleteAll();
    itemsNormalizados.deleteAll();
    donaciones.deleteAll();
    necesidades.deleteAll();
    jdbc.update("DELETE FROM donaciones.deposito");
    jdbc.update("DELETE FROM donaciones.donante");
    jdbc.update("DELETE FROM donaciones.entidad_beneficiaria");
    jdbc.update("DELETE FROM donaciones.persona");
    jdbc.update("DELETE FROM donaciones.direccion");
    jdbc.update("DELETE FROM donaciones.subcategoria");
    jdbc.update("DELETE FROM donaciones.categoria");

    UUID subcategoriaId = crearSubcategoria();
    UUID entidadId = crearEntidad();
    necesidad = NecesidadMother.extraordinaria(entidadId, subcategoriaId, 80);
    necesidades.save(necesidad);

    Bien fideos = BienMother.alimentoPerecedero("Fideos secos");
    Donacion original = DonacionMother.segmentada(crearDonante(), fideos, 30);
    donaciones.save(original);
    ItemDonacionNormalizado normalizado =
        itemsNormalizados.save(
            new ItemDonacionNormalizado(
                original.getId(), BienMother.aceptado(fideos, subcategoriaId), 30));
    DonacionIndependiente primera = independiente(normalizado, 20);
    DonacionIndependiente segunda = independiente(normalizado, 10);
    independientes.saveAll(List.of(primera, segunda));

    // Cada una se fragmenta (se pide menos de lo que tiene): la aprobación crea donaciones nuevas
    propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    propuesta.agregarFragmentacion(primera, 5);
    propuesta.agregarFragmentacion(segunda, 5);
    propuestas.save(propuesta);

    prepararBeneficiario(entidadId);
  }

  // Deja resolvibles los datos del beneficiario, para que la aprobación llegue hasta el broker
  private void prepararBeneficiario(UUID entidadId) {
    EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
    UUID juridicaId = UUID.randomUUID();
    when(entidad.juridicaId()).thenReturn(juridicaId);
    when(entidadesBeneficiariasRepository.findById(entidadId)).thenReturn(Optional.of(entidad));
    when(personasRepository.findById(juridicaId)).thenReturn(Optional.of(mock(Juridica.class)));
    when(donantesRepository.findById(any())).thenReturn(Optional.of(mock(Donante.class)));
  }

  private static DonacionIndependiente independiente(
      ItemDonacionNormalizado normalizado, int cantidad) {
    return new DonacionIndependiente(
        normalizado.getDonacionOriginalId(),
        List.of(
            new ItemDonacionIndependiente(normalizado.getId(), normalizado.getBien(), cantidad)));
  }

  private UUID crearDonante() {
    UUID id = UUID.randomUUID();
    jdbc.update("INSERT INTO donaciones.persona (id, tipo_persona) VALUES (?, 'HUMANA')", id);
    jdbc.update(
        "INSERT INTO donaciones.humana (id, nombre, apellido) VALUES (?, 'Ana', 'Prueba')", id);
    jdbc.update("INSERT INTO donaciones.donante (id, activo) VALUES (?, true)", id);
    return id;
  }

  private UUID crearEntidad() {
    UUID id = UUID.randomUUID();
    jdbc.update("INSERT INTO donaciones.persona (id, tipo_persona) VALUES (?, 'JURIDICA')", id);
    jdbc.update(
        "INSERT INTO donaciones.juridica (id, razon_social, tipo_juridico, rubro)"
            + " VALUES (?, 'Comedor', 'ONG', 'Social')",
        id);
    jdbc.update("INSERT INTO donaciones.entidad_beneficiaria (id, activo) VALUES (?, true)", id);
    return id;
  }

  private UUID crearSubcategoria() {
    UUID categoriaId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.categoria (id, nombre, con_uso, con_vencimiento, tipo_unidad)"
            + " VALUES (?, 'Alimentos', false, true, 'UNIDADES')",
        categoriaId);
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.subcategoria (id, id_categoria, nombre) VALUES (?, ?, 'Fideos')",
        id,
        categoriaId);
    return id;
  }

  private int contarDonaciones(String where) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM donaciones.donacion_independiente WHERE " + where, Integer.class);
  }

  @Test
  void
      aprobar_siElBrokerFallaEnLaSegundaFragmentacion_noQuedaNadaAsignadoYLaPropuestaSigueAbierta() {
    doNothing()
        .doThrow(new IllegalStateException("broker caído"))
        .when(logisticaBroker)
        .solicitarEntrega(any());

    assertThrows(
        IllegalStateException.class,
        () -> service.actualizarEstado(propuesta.getId(), EstadoPropuesta.APROBADA));

    // La primera fragmentación llegó a guardarse y a pedirse al broker, y después se revirtió
    verify(logisticaBroker, times(2)).solicitarEntrega(any());
    assertEquals(0, contarDonaciones("estado_actual = 'ASIGNACION_REALIZADA'"));
    assertEquals(2, contarDonaciones("true"));
    assertEquals(
        EstadoPropuesta.PENDIENTE,
        propuestas.findById(propuesta.getId()).orElseThrow().getEstado());
  }

  @Test
  void aprobar_cuandoTodoSale_confirmaLasAsignacionesYLaPropuestaJuntas() {
    service.actualizarEstado(propuesta.getId(), EstadoPropuesta.APROBADA);

    verify(logisticaBroker, times(2)).solicitarEntrega(any());
    assertEquals(2, contarDonaciones("estado_actual = 'ASIGNACION_REALIZADA'"));
    assertEquals(
        EstadoPropuesta.APROBADA, propuestas.findById(propuesta.getId()).orElseThrow().getEstado());
  }
}
