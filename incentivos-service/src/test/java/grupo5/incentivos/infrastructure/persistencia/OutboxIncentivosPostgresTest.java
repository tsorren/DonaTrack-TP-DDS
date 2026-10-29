package grupo5.incentivos.infrastructure.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.incentivos.config.RabbitMQConfig;
import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.fixtures.EventoDonacionMother;
import grupo5.incentivos.infrastructure.clients.NotificacionesFeignClient;
import grupo5.incentivos.infrastructure.persistencia.adapters.DonanteIncentivosRepositoryJpaAdapter;
import grupo5.incentivos.infrastructure.persistencia.adapters.EventosProcesadosJpaAdapter;
import grupo5.incentivos.infrastructure.persistencia.adapters.OutboxIncentivosJdbcAdapter;
import grupo5.incentivos.infrastructure.persistencia.mappers.DonanteIncentivosPersistenciaMapper;
import grupo5.incentivos.infrastructure.persistencia.mappers.MisionPersistenciaMapper;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.outbox.EntradaOutbox;
import grupo5.incentivos.models.outbox.EstadoOutbox;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import grupo5.incentivos.models.repositories.IEventosProcesados;
import grupo5.incentivos.models.repositories.IOutboxIncentivos;
import java.time.Duration;
import java.time.LocalDateTime;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * El outbox sobre una base Postgres real: que los avisos queden guardados junto con el donante y
 * que el relay pueda reservar las entradas sin que dos lectores tomen las mismas.
 */
@DataJpaTest(
    properties = {
      "incentivos.rabbitmq.enabled=true",
      "incentivos.outbox.enabled=true",
      "spring.datasource.hikari.schema=incentivos"
    })
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  MisionPersistenciaMapper.class,
  DonanteIncentivosPersistenciaMapper.class,
  DonanteIncentivosRepositoryJpaAdapter.class,
  EventosProcesadosJpaAdapter.class,
  OutboxIncentivosJdbcAdapter.class
})
// Sin transacción de test: cada operación debe confirmarse sola, como en producción.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class OutboxIncentivosPostgresTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("donatrack")
          .withUsername("incentivos_user")
          .withPassword("inc_pass_2026")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("init-db/01-init-schemas-roles.sql"),
              "/docker-entrypoint-initdb.d/01-init-schemas-roles.sql");

  // @EnableFeignClients de la clase principal registra este cliente también en el slice JPA.
  @MockitoBean private NotificacionesFeignClient notificacionesFeignClient;

  @Autowired private IEventosProcesados eventosProcesados;
  @Autowired private IOutboxIncentivos outbox;
  @Autowired private IDonanteIncentivosRepository donanteRepository;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void limpiar() {
    jdbc.update("DELETE FROM outbox_incentivos");
    jdbc.update("DELETE FROM evento_procesado");
    donanteRepository.deleteAll();
  }

  private DonanteIncentivos donanteConEventos() {
    // Con la misión de racha, esta donación cumple la misión y sube de categoría.
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorConMisionRacha(1);
    donante.registrarDonacion(EventoDonacionMother.valido());
    assertFalse(donante.getDomainEvents().isEmpty());
    return donante;
  }

  private int contarEntradas() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM outbox_incentivos", Integer.class);
  }

  @Test
  void guardarSiNoProcesado_guardaElDonanteYRegistraSusAvisosJuntos() {
    DonanteIncentivos donante = donanteConEventos();

    assertTrue(eventosProcesados.guardarSiNoProcesado(UUID.randomUUID(), donante));

    assertTrue(donanteRepository.findById(donante.getId()).isPresent());
    List<String> routingKeys =
        jdbc.queryForList("SELECT routing_key FROM outbox_incentivos", String.class);
    assertTrue(routingKeys.contains(RabbitMQConfig.ROUTING_KEY_MISION_CUMPLIDA));
    assertTrue(routingKeys.contains(RabbitMQConfig.ROUTING_KEY_SUBIO_CATEGORIA));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM outbox_incentivos WHERE estado <> 'PENDIENTE'", Integer.class));
  }

  @Test
  void guardarSiNoProcesado_conUnEventoRepetido_noDuplicaLosAvisos() {
    DonanteIncentivos donante = donanteConEventos();
    UUID eventoId = UUID.randomUUID();
    eventosProcesados.guardarSiNoProcesado(eventoId, donante);
    int avisos = contarEntradas();

    assertFalse(eventosProcesados.guardarSiNoProcesado(eventoId, donante));

    assertEquals(avisos, contarEntradas());
  }

  @Test
  void guardarSiNoProcesado_sinEventoId_guardaYRegistraLosAvisos() {
    DonanteIncentivos donante = donanteConEventos();

    assertTrue(eventosProcesados.guardarSiNoProcesado(null, donante));

    assertTrue(contarEntradas() > 0);
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM evento_procesado", Integer.class));
  }

  @Test
  void reclamarPendientes_reservaLasEntradasYNoLasEntregaDosVeces() {
    outbox.registrarInactividad(UUID.randomUUID(), 30);
    outbox.registrarInactividad(UUID.randomUUID(), 40);
    LocalDateTime ahora = LocalDateTime.now().plusSeconds(1);

    List<EntradaOutbox> primera = outbox.reclamarPendientes(ahora, Duration.ofSeconds(60), 10);
    List<EntradaOutbox> segunda = outbox.reclamarPendientes(ahora, Duration.ofSeconds(60), 10);

    assertEquals(2, primera.size());
    assertTrue(segunda.isEmpty());
    assertEquals(RabbitMQConfig.ROUTING_KEY_DONANTE_INACTIVO, primera.get(0).routingKey());
  }

  @Test
  void reclamarPendientes_respetaElLimiteYVuelveAEntregarLasVencidas() {
    outbox.registrarInactividad(UUID.randomUUID(), 30);
    outbox.registrarInactividad(UUID.randomUUID(), 40);
    outbox.registrarInactividad(UUID.randomUUID(), 50);
    LocalDateTime ahora = LocalDateTime.now().plusSeconds(1);

    List<EntradaOutbox> tomadas = outbox.reclamarPendientes(ahora, Duration.ofSeconds(60), 2);
    List<EntradaOutbox> tras = outbox.reclamarPendientes(ahora, Duration.ofSeconds(60), 10);
    List<EntradaOutbox> vencido =
        outbox.reclamarPendientes(ahora.plusMinutes(5), Duration.ofSeconds(60), 10);

    assertEquals(2, tomadas.size());
    assertEquals(1, tras.size());
    assertEquals(3, vencido.size());
  }

  @Test
  void actualizar_persisteElResultadoYLaEntradaPublicadaYaNoSeReclama() {
    outbox.registrarInactividad(UUID.randomUUID(), 30);
    LocalDateTime ahora = LocalDateTime.now().plusSeconds(1);
    EntradaOutbox entrada = outbox.reclamarPendientes(ahora, Duration.ofSeconds(1), 10).get(0);

    outbox.actualizar(entrada.publicada());

    assertEquals(
        EstadoOutbox.PUBLICADO.name(),
        jdbc.queryForObject(
            "SELECT estado FROM outbox_incentivos WHERE id = ?", String.class, entrada.id()));
    assertTrue(
        outbox.reclamarPendientes(ahora.plusMinutes(5), Duration.ofSeconds(1), 10).isEmpty());
  }
}
