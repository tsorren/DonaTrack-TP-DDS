package grupo5.donaciones.models.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.donaciones.fixtures.CategoriaMother;
import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.TipoNecesidad;
import grupo5.donaciones.models.repositories.impl.DonacionesIndependientesRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.NecesidadesRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.SubcategoriasRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.listados.CriterioNecesidades;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * La búsqueda paginada de necesidades vigentes en el repositorio en memoria (SPEC-11, G2, lote
 * {@code g2-necesidades}): activas, sin vencer y no satisfechas, con los filtros de la consulta
 * {@code necesidades} y el orden fijo por descripción e id. El adaptador JPA hace lo mismo en SQL.
 */
class NecesidadesVigentesEnMemoriaTest {

  private static final Paginado PRIMERA = new Paginado(0, 20);
  private static final LocalDate HOY = LocalDate.now(ZoneId.systemDefault());

  private final SubcategoriasRepositoryEnMemoria subcategorias =
      new SubcategoriasRepositoryEnMemoria();
  private final DonacionesIndependientesRepositoryEnMemoria independientes =
      new DonacionesIndependientesRepositoryEnMemoria();
  private final NecesidadesRepositoryEnMemoria necesidades =
      new NecesidadesRepositoryEnMemoria(subcategorias, independientes);

  private final UUID entidad = UUID.randomUUID();
  private Subcategoria arroz;
  private Subcategoria camperas;

  @BeforeEach
  void setUp() {
    Categoria alimentos = CategoriaMother.alimentos();
    Categoria ropa = CategoriaMother.ropa();
    arroz = subcategorias.save(CategoriaMother.arroz(alimentos));
    camperas = subcategorias.save(CategoriaMother.camperas(ropa));
  }

  private NecesidadExtraordinaria extraordinaria(String descripcion, Subcategoria sub, int pide) {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(sub.getId(), pide, descripcion, NecesidadMother.FECHA_LIMITE);
    necesidad.asociarAEntidad(entidad);
    necesidades.save(necesidad);
    return necesidad;
  }

  private NecesidadRecurrente recurrente(Subcategoria sub, int pide) {
    NecesidadRecurrente necesidad = NecesidadMother.recurrenteSemanal(entidad, sub.getId(), pide);
    necesidades.save(necesidad);
    return necesidad;
  }

  private List<UUID> ids(CriterioNecesidades criterio) {
    return necesidades.buscarVigentes(criterio).elementos().stream().map(Necesidad::getId).toList();
  }

  private static CriterioNecesidades sinFiltros(Paginado paginado) {
    return new CriterioNecesidades(null, null, null, null, HOY, paginado);
  }

  @Test
  void dejaAfueraLasInactivasLasVencidasYLasSatisfechas() {
    NecesidadExtraordinaria vigente = extraordinaria("Arroz", arroz, 10);
    NecesidadExtraordinaria inactiva = extraordinaria("Arroz de baja", arroz, 10);
    inactiva.desactivar();
    necesidades.save(inactiva);
    // La fecha límite tiene que ser futura al crearla: vence para una consulta de dentro de 5 días
    NecesidadExtraordinaria vencida =
        new NecesidadExtraordinaria(arroz.getId(), 10, "Arroz vencido", HOY.plusDays(1));
    vencida.asociarAEntidad(entidad);
    necesidades.save(vencida);
    NecesidadExtraordinaria satisfecha = extraordinaria("Arroz cubierto", arroz, 3);
    independientes.save(DonacionIndependienteMother.asignada(3, satisfecha));
    NecesidadExtraordinaria aMedias = extraordinaria("Arroz a medias", arroz, 5);
    independientes.save(DonacionIndependienteMother.asignada(2, aMedias));

    Tramo<Necesidad> tramo =
        necesidades.buscarVigentes(
            new CriterioNecesidades(null, null, null, null, HOY.plusDays(5), PRIMERA));

    // "Arroz" antes que "Arroz a medias"
    assertEquals(
        List.of(vigente.getId(), aMedias.getId()),
        tramo.elementos().stream().map(Necesidad::getId).toList());
    assertEquals(2, tramo.total());
  }

  @Test
  void unaRecurrenteSeSatisfaceConLoAsignadoASuPeriodoActual() {
    NecesidadRecurrente cubierta = recurrente(arroz, 4);
    independientes.save(DonacionIndependienteMother.asignada(4, cubierta));
    NecesidadRecurrente pendiente = recurrente(camperas, 4);
    independientes.save(DonacionIndependienteMother.asignada(1, pendiente));

    assertEquals(List.of(pendiente.getId()), ids(sinFiltros(PRIMERA)));
  }

  @Test
  void filtraPorEntidadSubcategoriaCategoriaYTipo() {
    NecesidadExtraordinaria deArroz = extraordinaria("Arroz", arroz, 10);
    NecesidadExtraordinaria deCamperas = extraordinaria("Camperas", camperas, 10);
    NecesidadRecurrente recurrenteDeArroz = recurrente(arroz, 10);
    NecesidadExtraordinaria ajena =
        new NecesidadExtraordinaria(arroz.getId(), 10, "Arroz ajeno", NecesidadMother.FECHA_LIMITE);
    ajena.asociarAEntidad(UUID.randomUUID());
    necesidades.save(ajena);

    assertEquals(
        List.of(deArroz.getId(), deCamperas.getId(), recurrenteDeArroz.getId()),
        ids(new CriterioNecesidades(entidad, null, null, null, HOY, PRIMERA)));
    assertEquals(
        List.of(deCamperas.getId()),
        ids(new CriterioNecesidades(null, null, camperas.getId(), null, HOY, PRIMERA)));
    assertEquals(
        List.of(deArroz.getId(), ajena.getId(), recurrenteDeArroz.getId()),
        ids(new CriterioNecesidades(null, arroz.getCategoriaId(), null, null, HOY, PRIMERA)));
    assertEquals(
        List.of(recurrenteDeArroz.getId()),
        ids(new CriterioNecesidades(null, null, null, TipoNecesidad.RECURRENTE, HOY, PRIMERA)));
    assertEquals(
        List.of(deArroz.getId()),
        ids(
            new CriterioNecesidades(
                entidad,
                arroz.getCategoriaId(),
                arroz.getId(),
                TipoNecesidad.EXTRAORDINARIA,
                HOY,
                PRIMERA)));
  }

  @Test
  void ordenaPorDescripcionSinTildesNiMayusculasYDesempataPorId() {
    NecesidadExtraordinaria zapallo = extraordinaria("zapallo", arroz, 10);
    NecesidadExtraordinaria aceite = extraordinaria("Áceite", arroz, 10);
    NecesidadExtraordinaria fideosUno = extraordinaria("Fideos", arroz, 10);
    NecesidadExtraordinaria fideosDos = extraordinaria("Fideos", arroz, 10);
    List<UUID> fideos =
        Stream.of(fideosUno.getId(), fideosDos.getId())
            .sorted(Comparator.comparing(UUID::toString))
            .toList();

    assertEquals(
        List.of(aceite.getId(), fideos.get(0), fideos.get(1), zapallo.getId()),
        ids(sinFiltros(PRIMERA)));
  }

  @Test
  void cortaLaPaginaPedidaYCuentaTodasLasQueCumplen() {
    extraordinaria("A", arroz, 10);
    NecesidadExtraordinaria b = extraordinaria("B", arroz, 10);
    NecesidadExtraordinaria c = extraordinaria("C", arroz, 10);
    NecesidadExtraordinaria d = extraordinaria("D", arroz, 10);

    Tramo<Necesidad> segunda = necesidades.buscarVigentes(sinFiltros(new Paginado(1, 2)));
    Tramo<Necesidad> fuera = necesidades.buscarVigentes(sinFiltros(new Paginado(5, 2)));

    assertEquals(
        List.of(c.getId(), d.getId()), segunda.elementos().stream().map(Necesidad::getId).toList());
    assertEquals(4, segunda.total());
    assertEquals(List.of(), fuera.elementos());
    assertEquals(4, fuera.total());
    assertEquals(List.of(b.getId()), ids(sinFiltros(new Paginado(1, 1))));
  }

  @Test
  void sinLosRepositoriosDeApoyo_laCategoriaNoEncuentraNadaYNingunaEstaSatisfecha() {
    NecesidadesRepositoryEnMemoria solo = new NecesidadesRepositoryEnMemoria();
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(arroz.getId(), 1, "Arroz", NecesidadMother.FECHA_LIMITE);
    solo.save(necesidad);

    assertEquals(1, solo.buscarVigentes(sinFiltros(PRIMERA)).total());
    assertEquals(
        0,
        solo.buscarVigentes(
                new CriterioNecesidades(null, arroz.getCategoriaId(), null, null, HOY, PRIMERA))
            .total());
  }
}
