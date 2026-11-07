package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.infrastructure.persistencia.mapper.DireccionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.ItemsNormalizadosPersistenciaMapper;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.CambioEstadoDonacion;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

/** Paso 7: donación (con depósito, ítems e historial) e ítems normalizados. */
@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  DireccionesPersistenciaMapper.class,
  DonacionesPersistenciaMapper.class,
  DonacionRepositoryJpaAdapter.class,
  ItemsNormalizadosPersistenciaMapper.class,
  ItemDonacionNormalizadoRepositoryJpaAdapter.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class DonacionesRepositoriesJpaTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private IDonacionesRepository donaciones;
  @Autowired private IItemDonacionNormalizadoRepository itemsNormalizados;
  @Autowired private JdbcTemplate jdbc;

  private UUID donanteId;
  private UUID subcategoriaId;

  @BeforeEach
  void limpiar() {
    itemsNormalizados.deleteAll();
    donaciones.deleteAll();
    jdbc.update("DELETE FROM donaciones.deposito");
    jdbc.update("DELETE FROM donaciones.donante");
    jdbc.update("DELETE FROM donaciones.persona");
    jdbc.update("DELETE FROM donaciones.direccion");
    jdbc.update("DELETE FROM donaciones.subcategoria");
    jdbc.update("DELETE FROM donaciones.categoria");
    donanteId = crearDonante();
    subcategoriaId = crearSubcategoria();
  }

  private UUID crearDonante() {
    UUID id = UUID.randomUUID();
    jdbc.update("INSERT INTO donaciones.persona (id, tipo_persona) VALUES (?, 'HUMANA')", id);
    jdbc.update(
        "INSERT INTO donaciones.humana (id, nombre, apellido) VALUES (?, 'Ana', 'Prueba')", id);
    jdbc.update("INSERT INTO donaciones.donante (id, activo) VALUES (?, true)", id);
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

  private int contar(String tabla) {
    return jdbc.queryForObject("SELECT count(*) FROM donaciones." + tabla, Integer.class);
  }

  @Test
  void guardarYLeer_donacionSegmentada_conservaItemsDepositoEstadoEHistorial() {
    Bien fideos = BienMother.alimentoPerecedero("Fideos secos");
    Donacion original = DonacionMother.segmentada(donanteId, fideos, 100);

    donaciones.save(original);
    Donacion leida = donaciones.findById(original.getId()).orElseThrow();

    assertEquals(donanteId, leida.getDonanteId());
    assertEquals("Donación de prueba", leida.getDescripcion());
    assertEquals(EstadoDonacion.SEGMENTADA, leida.getEstadoActual());
    assertEquals(original.getDepositoRecepcion(), leida.getDepositoRecepcion());
    assertEquals(original.getItems(), leida.getItems());
    assertEquals(2, leida.getHistorialEstados().size());
    CambioEstadoDonacion primero = leida.getHistorialEstados().getFirst();
    assertEquals(EstadoDonacion.CARGADA, primero.getEstadoAnterior());
    assertEquals(EstadoDonacion.NORMALIZADA, primero.getEstadoNuevo());
    assertTrue(leida.getDomainEvents().isEmpty());
  }

  @Test
  void guardar_alAvanzarElEstado_agregaAlHistorialSinDuplicarNiTocarLoAnterior() {
    Donacion donacion = DonacionMother.normalizada(donanteId, BienMother.simple("Arroz"), 5);
    donaciones.save(donacion);
    LocalDateTime primerCambio =
        donaciones
            .findById(donacion.getId())
            .orElseThrow()
            .getHistorialEstados()
            .getFirst()
            .getTimestamp();

    donacion.marcarSegmentada();
    donaciones.save(donacion);
    Donacion leida = donaciones.findById(donacion.getId()).orElseThrow();

    assertEquals(2, contar("cambio_estado_donacion"));
    assertEquals(primerCambio, leida.getHistorialEstados().getFirst().getTimestamp());
    assertEquals(EstadoDonacion.SEGMENTADA, leida.getEstadoActual());
  }

  @Test
  void guardar_dosDonacionesEnElMismoDeposito_compartenLaFila() {
    donaciones.save(DonacionMother.simple(donanteId));
    donaciones.save(DonacionMother.simple(donanteId));

    assertEquals(1, contar("deposito"));
    assertEquals(2, contar("donacion"));
  }

  @Test
  void guardar_alQuitarUnItem_loBorraDeLaBase() {
    Donacion donacion = DonacionMother.conItem(donanteId, BienMother.simple("Arroz"), 5);
    ItemDonacion segundo = new ItemDonacion(BienMother.mueble("Silla"), 2);
    donacion.agregarItem(segundo);
    donaciones.save(donacion);

    donacion.quitarItem(segundo);
    donaciones.save(donacion);

    assertEquals(1, contar("item_donacion"));
    assertEquals(1, donaciones.findById(donacion.getId()).orElseThrow().getItems().size());
  }

  @Test
  void guardar_pesoConMasDeTresDecimales_seRedondeaATres() {
    Bien liviano = new Bien("Té en hebras", null, null, null, 0.12345, 0.0006);
    donaciones.save(DonacionMother.conItem(donanteId, liviano, 1));

    Bien leido = donaciones.findAll().getFirst().getItems().getFirst().bien();

    assertEquals(0.123, leido.pesoUnitario());
    assertEquals(0.001, leido.volumenUnitario());
  }

  @Test
  void guardar_pesoQueSeRedondeaACero_noSeGuarda() {
    Bien casiNada = new Bien("Semillas", null, null, null, 0.0004, 0.01);
    Donacion donacion = DonacionMother.conItem(donanteId, casiNada, 1);

    // Redondeado a 0,000 ya no es un Bien válido: falla dentro de la transacción y no queda nada.
    // Por fuera de la app, el CHECK (peso_unitario > 0) de V9 lo rechaza igual.
    ValidationException ex =
        assertThrows(ValidationException.class, () -> donaciones.save(donacion));

    assertEquals(ErrorCatalog.DIMENSIONES_BIEN_INVALIDAS, ex.getError());
    assertEquals(0, contar("donacion"));
    assertEquals(0, contar("item_donacion"));
  }

  @Test
  void insertar_pesoCeroPorFueraDeLaApp_loRechazaLaBase() {
    Donacion donacion = DonacionMother.simple(donanteId);
    donaciones.save(donacion);
    UUID idItem = UUID.randomUUID();
    UUID idDonacion = donacion.getId();

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "INSERT INTO donaciones.item_donacion"
                    + " (id, id_donacion, descripcion, peso_unitario, volumen_unitario, cantidad)"
                    + " VALUES (?, ?, 'Semillas', 0, 0.01, 1)",
                idItem,
                idDonacion));
  }

  @Test
  void guardar_donacionDeUnDonanteInexistente_fallaPorClaveForanea() {
    Donacion huerfana = DonacionMother.simple(UUID.randomUUID());

    assertThrows(DataIntegrityViolationException.class, () -> donaciones.save(huerfana));
  }

  @Test
  void guardarYLeer_itemNormalizado_conservaElBienCompletoYSuNormalizacion() {
    Donacion donacion =
        DonacionMother.conItem(donanteId, BienMother.alimentoPerecedero("Fideos"), 3);
    donaciones.save(donacion);
    BienNormalizado bien =
        new BienNormalizado(
            BienMother.alimentoPerecedero("Fideos"),
            subcategoriaId,
            0.87,
            EstadoNormalizacion.PENDIENTE_REVISION);
    ItemDonacionNormalizado original = new ItemDonacionNormalizado(donacion.getId(), bien, 3);
    original.marcarComoSegmentado();

    itemsNormalizados.save(original);
    ItemDonacionNormalizado leido = itemsNormalizados.findById(original.getId()).orElseThrow();

    assertEquals(original.getBien(), leido.getBien());
    assertEquals(donacion.getId(), leido.getDonacionOriginalId());
    assertEquals(3, leido.getCantidad());
    assertTrue(leido.isSegmentado());
    assertTrue(itemsNormalizados.existsBySubcategoriaId(subcategoriaId));
    assertFalse(itemsNormalizados.existsBySubcategoriaId(UUID.randomUUID()));
  }

  @Test
  void buscarItemsNormalizados_porDonacionYPendientes_filtraEnLaBase() {
    Bien arroz = BienMother.simple("Arroz");
    Donacion una = DonacionMother.normalizada(donanteId, arroz, 5);
    Donacion otra = DonacionMother.normalizada(donanteId, arroz, 3);
    donaciones.saveAll(List.of(una, otra));
    ItemDonacionNormalizado aceptado =
        itemsNormalizados.save(
            new ItemDonacionNormalizado(
                una.getId(), BienMother.aceptado(arroz, subcategoriaId), 5));
    ItemDonacionNormalizado pendiente =
        itemsNormalizados.save(
            new ItemDonacionNormalizado(
                una.getId(), BienMother.pendienteRevision(arroz, subcategoriaId, 0.4), 2));
    ItemDonacionNormalizado deOtra =
        itemsNormalizados.save(
            new ItemDonacionNormalizado(
                otra.getId(), BienMother.pendienteRevision(arroz, subcategoriaId, 0.3), 3));

    assertEquals(
        Set.of(aceptado.getId(), pendiente.getId()),
        itemsNormalizados.findByDonacion(una.getId()).stream()
            .map(ItemDonacionNormalizado::getId)
            .collect(Collectors.toSet()));
    assertEquals(
        Set.of(pendiente.getId(), deOtra.getId()),
        itemsNormalizados.findPendientesDeRevision().stream()
            .map(ItemDonacionNormalizado::getId)
            .collect(Collectors.toSet()));
  }

  @Test
  void guardar_itemNormalizadoConSubcategoriaInexistente_fallaPorClaveForanea() {
    Donacion donacion = DonacionMother.simple(donanteId);
    donaciones.save(donacion);
    BienNormalizado bien =
        new BienNormalizado(
            BienMother.simple("Arroz"), UUID.randomUUID(), 0.5, EstadoNormalizacion.ACEPTADO);
    ItemDonacionNormalizado huerfano = new ItemDonacionNormalizado(donacion.getId(), bien, 1);

    assertThrows(DataIntegrityViolationException.class, () -> itemsNormalizados.save(huerfano));
  }
}
