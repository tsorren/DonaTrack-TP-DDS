package grupo5.logistica.infrastructure.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.infrastructure.persistencia.adapters.CamionRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.adapters.ChoferesRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.adapters.EntregasRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.adapters.EventosEntregaRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.adapters.RutasRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.adapters.SolicitudPlanificacionRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.adapters.SolicitudesTransicionEntregaRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.entities.CamionEntity;
import grupo5.logistica.infrastructure.persistencia.entities.ChoferEntity;
import grupo5.logistica.infrastructure.persistencia.entities.EntregaEntity;
import grupo5.logistica.infrastructure.persistencia.mappers.CamionPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.mappers.ChoferPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.mappers.EntregaPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.mappers.RutaPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.mappers.SolicitudPlanificacionPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataCamionRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataChoferRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataDireccionRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataEntregaRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataEventoEntregaRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataLocalidadRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataPaisRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataProvinciaRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataSolicitudTransicionEntregaRepository;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.entities.entregas.ConfirmacionRecepcion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.entregas.NoRecepcion;
import grupo5.logistica.models.entities.entregas.RegresoDeposito;
import grupo5.logistica.models.entities.entregas.RevisionEntrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.GestorDeRutas;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IChoferesRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IEventosEntregaRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import grupo5.logistica.models.repositories.ISolicitudesTransicionEntregaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
  CamionPersistenciaMapper.class,
  ChoferPersistenciaMapper.class,
  RutaPersistenciaMapper.class,
  EntregaPersistenciaMapper.class,
  SolicitudPlanificacionPersistenciaMapper.class,
  CamionRepositoryJpaAdapter.class,
  ChoferesRepositoryJpaAdapter.class,
  RutasRepositoryJpaAdapter.class,
  EntregasRepositoryJpaAdapter.class,
  SolicitudPlanificacionRepositoryJpaAdapter.class,
  SolicitudesTransicionEntregaRepositoryJpaAdapter.class,
  EventosEntregaRepositoryJpaAdapter.class
})
@Testcontainers
@DisabledIfDockerUnavailable
class RepositoriosLogisticaJpaTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("donatrack")
          .withUsername("logistica_user")
          .withPassword("logi_pass_2026")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("init-db/01-init-schemas-roles.sql"),
              "/docker-entrypoint-initdb.d/01-init-schemas-roles.sql");

  @Autowired private ICamionRepository camionRepository;
  @Autowired private IChoferesRepository choferesRepository;
  @Autowired private IRutasRepository rutasRepository;
  @Autowired private IEntregasRepository entregasRepository;
  @Autowired private ISolicitudPlanificacionRepository solicitudPlanificacionRepository;
  @Autowired private ISolicitudesTransicionEntregaRepository solicitudesTransicionRepository;
  @Autowired private IEventosEntregaRepository eventosEntregaRepository;

  @Autowired private SpringDataCamionRepository springDataCamionRepo;
  @Autowired private SpringDataChoferRepository springDataChoferRepo;
  @Autowired private SpringDataEntregaRepository springDataEntregaRepo;
  @Autowired private SpringDataPaisRepository springDataPaisRepo;
  @Autowired private SpringDataProvinciaRepository springDataProvinciaRepo;
  @Autowired private SpringDataLocalidadRepository springDataLocalidadRepo;
  @Autowired private SpringDataDireccionRepository springDataDireccionRepo;
  @Autowired private SpringDataSolicitudTransicionEntregaRepository springDataTransicionRepo;
  @Autowired private SpringDataEventoEntregaRepository springDataEventoRepo;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void limpiarTablas() {
    jdbcTemplate.execute(
        "TRUNCATE TABLE evento_entrega, solicitud_transicion_entrega, parada_ruta, "
            + "solicitud_planificacion_ruta, solicitud_planificacion, cambio_estado_entrega, "
            + "entrega, cambio_estado_ruta, ruta, cambio_estado_camion, camion, "
            + "cambio_estado_chofer, chofer, direccion, localidad, provincia, pais CASCADE");
  }

  @Test
  void deberiaUsarAdaptadoresJpaEnLugarDeMemoria() {
    assertInstanceOf(CamionRepositoryJpaAdapter.class, camionRepository);
    assertInstanceOf(ChoferesRepositoryJpaAdapter.class, choferesRepository);
    assertInstanceOf(RutasRepositoryJpaAdapter.class, rutasRepository);
    assertInstanceOf(EntregasRepositoryJpaAdapter.class, entregasRepository);
    assertInstanceOf(
        SolicitudPlanificacionRepositoryJpaAdapter.class, solicitudPlanificacionRepository);
    assertInstanceOf(
        SolicitudesTransicionEntregaRepositoryJpaAdapter.class, solicitudesTransicionRepository);
    assertInstanceOf(EventosEntregaRepositoryJpaAdapter.class, eventosEntregaRepository);
  }

  @Test
  void deberiaPersistirYRecuperarCamionEHistorialSinKeyChurn() {
    Camion camion = new Camion("AB123CD", 50.0f, 2000.0f, 3.2f);
    camion.deshabilitar();
    Camion guardado = camionRepository.save(camion);

    CamionEntity entityInicial = springDataCamionRepo.findById(guardado.getId()).orElseThrow();
    assertEquals(1, entityInicial.getHistorialEstado().size());
    UUID idCambioOriginal = entityInicial.getHistorialEstado().getFirst().getIdCambioEstadoCamion();

    guardado.habilitar();
    Camion actualizado = camionRepository.save(guardado);

    assertEquals(EstadoCamion.DISPONIBLE, actualizado.getEstado());
    assertEquals(2, actualizado.getHistorialEstado().size());

    CamionEntity entityActualizada = springDataCamionRepo.findById(guardado.getId()).orElseThrow();
    assertEquals(2, entityActualizada.getHistorialEstado().size());
    assertTrue(
        entityActualizada.getHistorialEstado().stream()
            .anyMatch(c -> idCambioOriginal.equals(c.getIdCambioEstadoCamion())),
        "El cambio de estado inicial debe conservar su PK sin key churn");
  }

  @Test
  void deberiaPersistirYRecuperarChoferEHistorialSinKeyChurn() {
    Chofer chofer = new Chofer("Juan", "Perez", "LIC-001", "1122334455");
    chofer.cambiarEstado(EstadoChofer.DESHABILITADO);
    Chofer guardado = choferesRepository.save(chofer);

    ChoferEntity entityInicial = springDataChoferRepo.findById(guardado.getId()).orElseThrow();
    assertEquals(1, entityInicial.getHistorialEstados().size());
    UUID idCambioOriginal =
        entityInicial.getHistorialEstados().getFirst().getIdCambioEstadoChofer();

    guardado.cambiarEstado(EstadoChofer.DISPONIBLE);
    Chofer actualizado = choferesRepository.save(guardado);

    assertEquals(EstadoChofer.DISPONIBLE, actualizado.getEstado());
    assertEquals(2, actualizado.getHistorialEstados().size());

    ChoferEntity entityActualizada = springDataChoferRepo.findById(guardado.getId()).orElseThrow();
    assertEquals(2, entityActualizada.getHistorialEstados().size());
    assertTrue(
        entityActualizada.getHistorialEstados().stream()
            .anyMatch(c -> idCambioOriginal.equals(c.getIdCambioEstadoChofer())),
        "El cambio de estado inicial del chofer debe conservar su PK sin key churn");
  }

  @Test
  void deberiaPersistirEntregaReutilizandoPaisProvinciaLocalidadSinDuplicarDireccion() {
    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion dir1 = new Direccion("Av. Rivadavia", 1200, 1, "A", "1033", caba);
    Direccion dir2 = new Direccion("Av. Corrientes", 3400, null, null, "1194", caba);

    Entrega entrega1 = new Entrega(UUID.randomUUID(), UUID.randomUUID(), dir1, 15.0f, 1.5f);
    Entrega entrega2 = new Entrega(UUID.randomUUID(), UUID.randomUUID(), dir2, 20.0f, 2.0f);

    Entrega guardada1 = entregasRepository.save(entrega1);
    entregasRepository.save(entrega2);

    assertEquals(1, springDataPaisRepo.count());
    assertEquals(1, springDataProvinciaRepo.count());
    assertEquals(1, springDataLocalidadRepo.count());
    assertEquals(2, springDataDireccionRepo.count());

    EntregaEntity entityInicial = springDataEntregaRepo.findById(guardada1.getId()).orElseThrow();
    UUID idDireccionOriginal = entityInicial.getDireccion().getIdDireccion();

    guardada1.iniciarRuta("Chofer Juan");
    guardada1.confirmarEntrega("Beneficiario");
    guardada1.adjuntarFotoRecepcion("https://donatrack.org/foto.jpg");
    Entrega actualizada1 = entregasRepository.save(guardada1);

    assertEquals(EstadoEntrega.ENTREGADA, actualizada1.getEstadoActual());
    assertEquals(2, actualizada1.getHistorialEstado().size());
    assertEquals(2, springDataDireccionRepo.count());

    EntregaEntity entityActualizada =
        springDataEntregaRepo.findById(guardada1.getId()).orElseThrow();
    assertEquals(idDireccionOriginal, entityActualizada.getDireccion().getIdDireccion());
  }

  @Test
  void deberiaDerivarRutaIdEnCamionYChoferYConservarElOrdenDeVisitaDeLaRuta() {
    Camion camion = camionRepository.save(new Camion("CD456EF", 80.0f, 4000.0f, 3.5f));
    Chofer chofer = choferesRepository.save(new Chofer("Maria", "Gomez", "LIC-002", "1199887766"));

    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion dir = new Direccion("Medrano", 951, null, null, "1179", caba);

    UUID idMenor = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID idMayor = UUID.fromString("00000000-0000-0000-0000-000000000002");

    Entrega entregaMayor =
        entregasRepository.save(
            new Entrega(
                idMayor,
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                dir,
                EstadoEntrega.PENDIENTE,
                List.of(),
                null,
                null,
                null,
                10.0f,
                1.0f,
                null));
    Entrega entregaMenor =
        entregasRepository.save(
            new Entrega(
                idMenor,
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                dir,
                EstadoEntrega.PENDIENTE,
                List.of(),
                null,
                null,
                null,
                12.0f,
                1.2f,
                null));

    Ruta ruta = new Ruta(LocalDate.of(2026, 10, 5), chofer.getId(), camion.getId());
    GestorDeRutas.agregarEntrega(ruta, entregaMayor);
    GestorDeRutas.agregarEntrega(ruta, entregaMenor);

    rutasRepository.save(ruta);
    entregaMayor = entregasRepository.save(entregaMayor);
    entregaMenor = entregasRepository.save(entregaMenor);

    Ruta rutaRecuperada = rutasRepository.findById(ruta.getId()).orElseThrow();
    // Se agregó primero la de id mayor: el orden de visita no depende de los ids.
    assertEquals(List.of(idMayor, idMenor), rutaRecuperada.getEntregaIds());

    GestorDeRutas.iniciarRuta(
        rutaRecuperada, camion, chofer, List.of(entregaMenor, entregaMayor), "Operador");
    rutasRepository.save(rutaRecuperada);
    camionRepository.save(camion);
    choferesRepository.save(chofer);
    entregaMenor = entregasRepository.save(entregaMenor);
    entregaMayor = entregasRepository.save(entregaMayor);

    Camion camionEnRuta = camionRepository.findById(camion.getId()).orElseThrow();
    Chofer choferEnRuta = choferesRepository.findById(chofer.getId()).orElseThrow();
    assertEquals(ruta.getId(), camionEnRuta.getRutaId());
    assertEquals(ruta.getId(), choferEnRuta.getRutaId());

    Entrega e1 = entregasRepository.findById(idMenor).orElseThrow();
    Entrega e2 = entregasRepository.findById(idMayor).orElseThrow();
    e1.confirmarEntrega("Receptor 1");
    e2.confirmarEntrega("Receptor 2");
    e1 = entregasRepository.save(e1);
    e2 = entregasRepository.save(e2);

    Ruta rutaEnTraslado = rutasRepository.findById(ruta.getId()).orElseThrow();
    GestorDeRutas.completarRuta(rutaEnTraslado, camionEnRuta, choferEnRuta, List.of(e1, e2));
    rutasRepository.save(rutaEnTraslado);
    camionRepository.save(camionEnRuta);
    choferesRepository.save(choferEnRuta);

    Camion camionDisponible = camionRepository.findById(camion.getId()).orElseThrow();
    Chofer choferDisponible = choferesRepository.findById(chofer.getId()).orElseThrow();
    assertNull(camionDisponible.getRutaId());
    assertNull(choferDisponible.getRutaId());
  }

  @Test
  void deberiaPersistirSolicitudPlanificacionYActualizarEstado() {
    Camion camion = camionRepository.save(new Camion("CD789EF", 60.0f, 3000.0f, 3.0f));
    Chofer chofer = choferesRepository.save(new Chofer("Luis", "Diaz", "LIC-004", "1133445566"));
    Ruta ruta =
        rutasRepository.save(new Ruta(LocalDate.of(2026, 10, 6), chofer.getId(), camion.getId()));

    SolicitudPlanificacion solicitud =
        new SolicitudPlanificacion(
            UUID.randomUUID(),
            LocalDate.of(2026, 10, 6),
            3,
            "http://localhost:8083/api/logistica/callback/rutas");

    SolicitudPlanificacion guardada = solicitudPlanificacionRepository.save(solicitud);
    assertEquals(EstadoSolicitud.PENDIENTE, guardada.getEstado());
    assertNotNull(guardada.getVersion());

    UUID rutaGenerada = ruta.getId();
    guardada.procesarResultados(List.of(rutaGenerada));
    SolicitudPlanificacion procesada = solicitudPlanificacionRepository.save(guardada);

    assertEquals(EstadoSolicitud.PROCESADA, procesada.getEstado());
    assertEquals(List.of(rutaGenerada), procesada.getRutasGeneradas());
  }

  @Test
  void deberiaLanzarObjectOptimisticLockingFailureExceptionAnteVersionObsoleta() {
    Camion inicial = camionRepository.save(new Camion("EF789GH", 60.0f, 3000.0f, 3.0f));

    Camion copia1 = camionRepository.findById(inicial.getId()).orElseThrow();
    Camion copia2Obsoleta = camionRepository.findById(inicial.getId()).orElseThrow();

    copia1.deshabilitar();
    camionRepository.save(copia1);

    copia2Obsoleta.deshabilitar();
    assertThrows(
        ObjectOptimisticLockingFailureException.class, () -> camionRepository.save(copia2Obsoleta));
  }

  @Test
  void deberiaPersistirAuditoriasDeSolicitudTransicionYEventosDeEntrega() {
    Camion camion = camionRepository.save(new Camion("GH123IJ", 70.0f, 3500.0f, 3.2f));
    Chofer chofer = choferesRepository.save(new Chofer("Ana", "Lopez", "LIC-003", "1144556677"));
    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion dir = new Direccion("Mozart", 2300, null, null, "1407", caba);

    Entrega entrega =
        entregasRepository.save(new Entrega(UUID.randomUUID(), UUID.randomUUID(), dir, 8.0f, 0.8f));
    Ruta ruta = new Ruta(LocalDate.of(2026, 10, 7), chofer.getId(), camion.getId());
    GestorDeRutas.agregarEntrega(ruta, entrega);
    ruta = rutasRepository.save(ruta);
    entrega = entregasRepository.save(entrega);

    solicitudesTransicionRepository.registrar(
        new ConfirmacionRecepcion(entrega, "Chofer Ana", "https://donatrack.org/evidencia.png"));
    solicitudesTransicionRepository.registrar(
        new NoRecepcion(entrega, "Chofer Ana", "Ausente", true));
    solicitudesTransicionRepository.registrar(new RevisionEntrega(entrega, "Supervisor"));
    solicitudesTransicionRepository.registrar(new RegresoDeposito(entrega, "Deposito"));

    assertEquals(4, springDataTransicionRepo.count());

    eventosEntregaRepository.registrarRutaAsignada(
        new EventoRutaAsignada(ruta.getId(), entrega.getId()), entrega);
    eventosEntregaRepository.registrarEntregaExitosa(
        new EntregaConfirmada(entrega.getId(), entrega.getIdDonacion(), ruta.getId()));
    eventosEntregaRepository.registrarEntregaFallida(
        new EntregaFallida(entrega.getId(), entrega.getIdDonacion(), "No atendieron", true));

    assertEquals(3, springDataEventoRepo.count());
  }
}
