package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.infrastructure.persistencia.mapper.DireccionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesIndependientesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.ItemsNormalizadosPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.NecesidadesPersistenciaMapper;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donaciones.events.DonacionNormalizada;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.ports.Segmentador;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.IDonacionesEventPublisher;
import grupo5.donaciones.services.impl.SegmentacionService;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Segmentar una donación contra Postgres real y con el proxy transaccional de Spring: las
 * donaciones independientes, los ítems marcados y el estado de la donación se confirman juntos. Si
 * el mensaje se reentrega después de una falla, no quedan donaciones independientes duplicadas.
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
  SegmentacionService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class SegmentacionTransaccionalJpaTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private SegmentacionService service;
  @Autowired private IDonacionesIndependientesRepository independientes;
  @Autowired private IDonacionesRepository donaciones;
  @Autowired private IItemDonacionNormalizadoRepository itemsNormalizados;
  @Autowired private INecesidadesRepository necesidades;
  @Autowired private JdbcTemplate jdbc;

  @MockitoBean private Segmentador segmentador;
  @MockitoBean private ICategoriasRepository categoriasRepository;
  @MockitoBean private ISubcategoriasRepository subcategoriasRepository;
  @MockitoBean private IDonacionesEventPublisher donacionesEventPublisher;
  @MockitoSpyBean private ItemDonacionNormalizadoRepositoryJpaAdapter itemsEspiados;

  private Donacion donacion;

  @BeforeEach
  void preparar() {
    independientes.deleteAll();
    itemsNormalizados.deleteAll();
    donaciones.deleteAll();
    necesidades.deleteAll();
    jdbc.update("DELETE FROM donaciones.deposito");
    jdbc.update("DELETE FROM donaciones.donante");
    jdbc.update("DELETE FROM donaciones.persona");
    jdbc.update("DELETE FROM donaciones.direccion");
    jdbc.update("DELETE FROM donaciones.subcategoria");
    jdbc.update("DELETE FROM donaciones.categoria");

    UUID subcategoriaId = crearSubcategoria();
    Bien fideos = BienMother.alimentoPerecedero("Fideos secos");
    donacion = DonacionMother.normalizada(crearDonante(), fideos, 30);
    donaciones.save(donacion);
    ItemDonacionNormalizado normalizado =
        itemsNormalizados.save(
            new ItemDonacionNormalizado(
                donacion.getId(), BienMother.aceptado(fideos, subcategoriaId), 30));
    when(segmentador.segmentar(any()))
        .thenReturn(
            List.of(
                new DonacionIndependiente(
                    donacion.getId(),
                    List.of(
                        new ItemDonacionIndependiente(
                            normalizado.getId(), normalizado.getBien(), 30)))));
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

  private int contarIndependientes() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM donaciones.donacion_independiente", Integer.class);
  }

  private DonacionNormalizada evento() {
    return new DonacionNormalizada(donacion.getId(), donacion.getDonanteId());
  }

  @Test
  void segmentar_siFallaAlMarcarLosItems_noQuedanDonacionesIndependientesDuplicables() {
    doThrow(new IllegalStateException("falla simulada al marcar el ítem"))
        .when(itemsEspiados)
        .save(any(ItemDonacionNormalizado.class));

    assertThrows(IllegalStateException.class, () -> service.procesarDonacionNormalizada(evento()));

    // Las donaciones independientes ya se habían guardado, y se revirtieron junto con el resto
    assertEquals(0, contarIndependientes());
    assertFalse(itemsNormalizados.findByDonacion(donacion.getId()).getFirst().isSegmentado());
    assertEquals(
        EstadoDonacion.NORMALIZADA,
        donaciones.findById(donacion.getId()).orElseThrow().getEstadoActual());
  }

  @Test
  void segmentar_cuandoTodoSale_confirmaDonacionesItemsYEstadoJuntos() {
    service.procesarDonacionNormalizada(evento());

    assertEquals(1, contarIndependientes());
    assertTrue(itemsNormalizados.findByDonacion(donacion.getId()).getFirst().isSegmentado());
    assertEquals(
        EstadoDonacion.SEGMENTADA,
        donaciones.findById(donacion.getId()).orElseThrow().getEstadoActual());
  }
}
