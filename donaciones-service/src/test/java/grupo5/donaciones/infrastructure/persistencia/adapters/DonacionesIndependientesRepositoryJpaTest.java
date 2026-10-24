package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.infrastructure.persistencia.mapper.DireccionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesIndependientesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.ItemsNormalizadosPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.NecesidadesPersistenciaMapper;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.CambioEstado;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.Entregada;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.SolicitudCambioEstadoDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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

/** Paso 8: donación independiente, sus ítems, sus estados y su historial. */
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
  DonacionIndependienteRepositoryJpaAdapter.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class DonacionesIndependientesRepositoryJpaTest {

  private static final String ACTOR = "admin@donatrack.org";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private IDonacionesIndependientesRepository independientes;
  @Autowired private IDonacionesRepository donaciones;
  @Autowired private IItemDonacionNormalizadoRepository itemsNormalizados;
  @Autowired private INecesidadesRepository necesidades;
  @Autowired private JdbcTemplate jdbc;

  private UUID subcategoriaId;
  private UUID entidadId;
  private ItemDonacionNormalizado normalizado;

  @BeforeEach
  void preparar() {
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
    subcategoriaId = crearSubcategoria();
    entidadId = crearEntidad();

    Bien fideos = BienMother.alimentoPerecedero("Fideos secos");
    Donacion donacion = DonacionMother.segmentada(crearDonante(), fideos, 30);
    donaciones.save(donacion);
    normalizado =
        itemsNormalizados.save(
            new ItemDonacionNormalizado(
                donacion.getId(), BienMother.aceptado(fideos, subcategoriaId), 30));
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

  private DonacionIndependiente nueva(int cantidad) {
    return new DonacionIndependiente(
        normalizado.getDonacionOriginalId(),
        List.of(
            new ItemDonacionIndependiente(normalizado.getId(), normalizado.getBien(), cantidad)));
  }

  private NecesidadRecurrente recurrente() {
    NecesidadRecurrente necesidad =
        NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 50);
    necesidades.save(necesidad);
    return necesidad;
  }

  private int contar(String tabla) {
    return jdbc.queryForObject("SELECT count(*) FROM donaciones." + tabla, Integer.class);
  }

  private static List<TipoEstadoDonacion> nuevos(DonacionIndependiente d) {
    return d.getHistorial().stream().map(c -> c.getEstadoNuevo().getTipo()).toList();
  }

  @Test
  void guardarYLeer_enDeposito_reconstituyeElBienDesdeElItemNormalizado() {
    DonacionIndependiente original = nueva(10);

    independientes.save(original);
    DonacionIndependiente leida = independientes.findById(original.getId()).orElseThrow();

    assertEquals(original.getDonacionOriginalId(), leida.getDonacionOriginalId());
    assertEquals(original.getItems(), leida.getItems());
    assertEquals(subcategoriaId, leida.getSubcategoriaId());
    assertEquals(TipoEstadoDonacion.EN_DEPOSITO, leida.getEstadoActual().getTipo());
    assertTrue(leida.getHistorial().isEmpty());
    assertNull(leida.getNecesidadId());
    assertEquals(
        original.getFechaRegistro().truncatedTo(ChronoUnit.MILLIS),
        leida.getFechaRegistro().truncatedTo(ChronoUnit.MILLIS));
    assertTrue(leida.getDomainEvents().isEmpty());
    // El estado inicial también es una fila (D-21)
    assertEquals(1, contar("estado_donacion_independiente"));
    assertEquals(
        "Fideos secos",
        jdbc.queryForObject(
            "SELECT bien_descripcion FROM donaciones.item_donacion_independiente", String.class));
  }

  @Test
  void guardar_asignadaAUnaRecurrente_persisteNecesidadPeriodoYEntidadDeEsaNecesidad() {
    NecesidadRecurrente necesidad = recurrente();
    DonacionIndependiente donacion = nueva(10);
    donacion.asignar(ACTOR, necesidad.destinoDeAsignacion());

    independientes.save(donacion);
    DonacionIndependiente leida = independientes.findById(donacion.getId()).orElseThrow();

    // D3: las tres columnas salen de la misma necesidad
    assertEquals(necesidad.getId(), leida.getNecesidadId());
    assertEquals(necesidad.periodoActualId(), leida.getPeriodoId());
    assertEquals(necesidad.getEntidadId(), leida.getEntidadBeneficiariaId());
    assertEquals(TipoEstadoDonacion.ASIGNACION_REALIZADA, leida.getEstadoActual().getTipo());
    CambioEstado cambio = leida.getHistorial().getFirst();
    assertEquals(TipoEstadoDonacion.EN_DEPOSITO, cambio.getEstadoAnterior().getTipo());
    assertEquals(ACTOR, cambio.getActor());
  }

  @Test
  void guardar_alAvanzarElEstado_agregaUnaFilaDeEstadoYUnCambioPorTransicion() {
    DonacionIndependiente donacion = nueva(10);
    independientes.save(donacion);
    donacion.asignar(ACTOR, recurrente().destinoDeAsignacion());
    independientes.save(donacion);

    DonacionIndependiente leida = independientes.findById(donacion.getId()).orElseThrow();
    leida.planificarRuta(ACTOR);
    independientes.save(leida);

    assertEquals(2, contar("cambio_estado_donacion_independiente"));
    assertEquals(3, contar("estado_donacion_independiente"));
    assertEquals(
        List.of(TipoEstadoDonacion.ASIGNACION_REALIZADA, TipoEstadoDonacion.LISTA_PARA_ENTREGAR),
        nuevos(independientes.findById(donacion.getId()).orElseThrow()));
  }

  @Test
  void guardarYLeer_fallaReplanificable_conservaElOrdenLaJustificacionYElDestino() {
    NecesidadRecurrente necesidad = recurrente();
    DonacionIndependiente donacion = nueva(10);
    donacion.asignar(ACTOR, necesidad.destinoDeAsignacion());
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    // Dos transiciones en la misma operación: pueden compartir el timestamp
    donacion.cambiarEstado(
        new SolicitudCambioEstadoDonacionIndependiente(
            TipoEstadoDonacion.ENTREGA_FALLIDA, null, "No atendieron", null, null, true, ACTOR));

    independientes.save(donacion);
    DonacionIndependiente leida = independientes.findById(donacion.getId()).orElseThrow();

    assertEquals(
        List.of(
            TipoEstadoDonacion.ASIGNACION_REALIZADA,
            TipoEstadoDonacion.LISTA_PARA_ENTREGAR,
            TipoEstadoDonacion.EN_TRASLADO,
            TipoEstadoDonacion.ENTREGA_FALLIDA,
            TipoEstadoDonacion.ASIGNACION_REALIZADA),
        nuevos(leida));
    // D-22: la justificación queda en el cambio que registró la falla
    assertEquals("No atendieron", leida.getHistorial().get(3).getJustificacion());
    // D2: el estado actual coincide con el último cambio del historial
    assertEquals(
        leida.getHistorial().getLast().getEstadoNuevo().getTipo(),
        leida.getEstadoActual().getTipo());
    // Se reintenta hacia el mismo destino
    assertEquals(necesidad.getId(), leida.getNecesidadId());
  }

  @Test
  void guardarYLeer_entregada_conservaLaPatenteDelCamion() {
    DonacionIndependiente donacion = nueva(10);
    donacion.asignar(ACTOR, recurrente().destinoDeAsignacion());
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    donacion.cambiarEstado(
        new SolicitudCambioEstadoDonacionIndependiente(
            TipoEstadoDonacion.ENTREGADA, null, null, null, "AB123CD", null, ACTOR));

    independientes.save(donacion);
    DonacionIndependiente leida = independientes.findById(donacion.getId()).orElseThrow();

    Entregada entregada = assertInstanceOf(Entregada.class, leida.getEstadoActual());
    assertEquals("AB123CD", entregada.getPatenteCamion());
  }

  @Test
  void retornarAlDeposito_limpiaElDestinoYDejaDeContarEnElAcumulado() {
    NecesidadRecurrente necesidad = recurrente();
    DonacionIndependiente donacion = nueva(10);
    donacion.asignar(ACTOR, necesidad.destinoDeAsignacion());
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    donacion.registrarFalla("Dirección inexistente", ACTOR);
    independientes.save(donacion);
    UUID periodoId = necesidad.periodoActualId();
    assertEquals(10, independientes.sumarCantidadAsignada(necesidad.getId(), periodoId));

    donacion.retornar(ACTOR);
    independientes.save(donacion);

    assertEquals(0, independientes.sumarCantidadAsignada(necesidad.getId(), periodoId));
    DonacionIndependiente leida = independientes.findById(donacion.getId()).orElseThrow();
    assertNull(leida.getNecesidadId());
    assertNull(leida.getPeriodoId());
    assertNull(leida.getEntidadBeneficiariaId());
    assertEquals(
        List.of(donacion.getId()),
        independientes.findEnDeposito().stream().map(DonacionIndependiente::getId).toList());
  }

  @Test
  void sumarCantidadAsignada_separaPeriodosYNecesidadesSinPeriodo() {
    NecesidadRecurrente recurrente = recurrente();
    NecesidadExtraordinaria extraordinaria =
        NecesidadMother.extraordinaria(entidadId, subcategoriaId, 80);
    necesidades.save(extraordinaria);
    DonacionIndependiente aRecurrente = nueva(7);
    aRecurrente.asignar(ACTOR, recurrente.destinoDeAsignacion());
    DonacionIndependiente aExtraordinaria = nueva(4);
    aExtraordinaria.asignar(ACTOR, extraordinaria.destinoDeAsignacion());
    independientes.saveAll(List.of(aRecurrente, aExtraordinaria, nueva(9)));

    assertEquals(
        7, independientes.sumarCantidadAsignada(recurrente.getId(), recurrente.periodoActualId()));
    assertEquals(0, independientes.sumarCantidadAsignada(recurrente.getId(), UUID.randomUUID()));
    assertEquals(4, independientes.sumarCantidadAsignada(extraordinaria.getId(), null));
    assertEquals(1, independientes.findEnDeposito().size());
  }

  @Test
  void contarPorEntidadRegistradasDesde_cuentaSoloLasAsignadasRegistradasDespues() {
    DonacionIndependiente asignada = nueva(5);
    asignada.asignar(ACTOR, recurrente().destinoDeAsignacion());
    independientes.saveAll(List.of(asignada, nueva(3)));

    assertEquals(
        Map.of(entidadId, 1),
        independientes.contarPorEntidadRegistradasDesde(LocalDateTime.now().minusDays(1)));
    assertEquals(Map.of(entidadId, 1), independientes.contarPorEntidadRegistradasDesde(null));
    assertTrue(
        independientes.contarPorEntidadRegistradasDesde(LocalDateTime.now().plusDays(1)).isEmpty());
  }

  @Test
  void guardar_trasFragmentarse_reemplazaLosItemsDeLaOriginalYGuardaElFragmento() {
    DonacionIndependiente original = nueva(30);
    independientes.save(original);

    DonacionIndependiente fragmento = original.fragmentarse(12);
    independientes.saveAll(List.of(original, fragmento));

    assertEquals(18, independientes.findById(original.getId()).orElseThrow().getCantidad());
    DonacionIndependiente leido = independientes.findById(fragmento.getId()).orElseThrow();
    assertEquals(12, leido.getCantidad());
    assertEquals(normalizado.getId(), leido.getItems().getFirst().itemNormalizadoId());
    assertEquals(2, contar("item_donacion_independiente"));
  }

  @Test
  void actualizar_itemConUnItemNormalizadoInexistente_loRechazaLaBase() {
    independientes.save(nueva(10));

    // Por SQL: por JPA, Hibernate ya rechaza la referencia al no encontrar el ítem normalizado
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "UPDATE donaciones.item_donacion_independiente SET id_item_normalizado = ?",
                UUID.randomUUID()));
  }

  @Test
  void borrar_itemNormalizadoUsadoPorUnaDonacionIndependiente_loRechazaLaBase() {
    independientes.save(nueva(10));

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "DELETE FROM donaciones.item_donacion_normalizado WHERE id = ?",
                normalizado.getId()));
  }

  @Test
  void insertar_periodoSinNecesidad_loRechazaLaBase() {
    DonacionIndependiente donacion = nueva(10);
    NecesidadRecurrente necesidad = recurrente();
    independientes.save(donacion);

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "UPDATE donaciones.donacion_independiente SET id_periodo = ? WHERE id = ?",
                necesidad.periodoActualId(),
                donacion.getId()));
  }
}
