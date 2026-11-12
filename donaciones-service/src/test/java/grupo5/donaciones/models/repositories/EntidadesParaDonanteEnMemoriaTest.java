package grupo5.donaciones.models.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.donaciones.dto.entidadBeneficiaria.FiltroOrganizaciones;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.CategoriaMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.DestinoAsignacion;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.repositories.impl.DonacionesIndependientesRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.DonacionesRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.EntidadesBeneficiariasRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.NecesidadesRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.PersonasRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.SubcategoriasRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.listados.CriterioEntidades;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * R11-12 en el repositorio en memoria: "pide algo de la categoría" y "recibió una donación de ese
 * donante" filtran todas las entidades, no una página (spec p1-donante-listas, A2 a A5).
 */
class EntidadesParaDonanteEnMemoriaTest {

  private static final Paginado PRIMERA = new Paginado(0, 20);
  // El día que mira el filtro: unos días después de hoy, así una extraordinaria creada hoy (con
  // fecha límite futura) ya puede estar vencida para la consulta
  private static final LocalDate HOY = LocalDate.now(ZoneId.systemDefault()).plusDays(10);

  private final PersonasRepositoryEnMemoria personas = new PersonasRepositoryEnMemoria();
  private final NecesidadesRepositoryEnMemoria necesidades = new NecesidadesRepositoryEnMemoria();
  private final SubcategoriasRepositoryEnMemoria subcategorias =
      new SubcategoriasRepositoryEnMemoria();
  private final DonacionesRepositoryEnMemoria donaciones = new DonacionesRepositoryEnMemoria();
  private final DonacionesIndependientesRepositoryEnMemoria independientes =
      new DonacionesIndependientesRepositoryEnMemoria(donaciones, subcategorias);
  private EntidadesBeneficiariasRepositoryEnMemoria entidades;

  private final Categoria alimentos = CategoriaMother.alimentos();
  private Subcategoria arroz;
  private final UUID donante = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    entidades =
        new EntidadesBeneficiariasRepositoryEnMemoria(
            personas, necesidades, subcategorias, independientes, donaciones);
    arroz = subcategorias.save(CategoriaMother.arroz(alimentos));
  }

  private UUID entidad(String razonSocial) {
    Juridica juridica =
        new Juridica(PersonaMother.juanPerez(), razonSocial, TipoJuridico.ONG, "Social");
    personas.save(juridica);
    return entidades.save(new EntidadBeneficiaria(juridica.getId())).getId();
  }

  private NecesidadExtraordinaria pide(UUID entidadId, LocalDate hasta) {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(arroz.getId(), 10, "Arroz para el comedor", hasta);
    necesidad.asociarAEntidad(entidadId);
    necesidades.save(necesidad);
    return necesidad;
  }

  private void donoA(UUID donanteId, UUID entidadId) {
    Donacion donacion = donaciones.save(DonacionMother.simple(donanteId));
    DonacionIndependiente independiente =
        new DonacionIndependiente(
            donacion.getId(),
            List.of(
                new ItemDonacionIndependiente(
                    UUID.randomUUID(),
                    BienMother.aceptado(BienMother.simple("Arroz"), arroz.getId()),
                    1)));
    if (entidadId != null) {
      independiente.asignar("admin", new DestinoAsignacion(null, null, entidadId));
    }
    independientes.save(independiente);
  }

  private List<UUID> ids(CriterioEntidades criterio) {
    return entidades.buscar(criterio).elementos().stream().map(EntidadBeneficiaria::getId).toList();
  }

  @Test
  void porCategoria_soloLasQuePidenAlgoActivoYSinVencer_A2() {
    UUID activa = entidad("Comedor Activo");
    UUID deBaja = entidad("Comedor de Baja");
    UUID vencida = entidad("Comedor Vencido");
    entidad("Comedor Sin Pedidos");
    pide(activa, HOY.plusDays(5));
    pide(deBaja, HOY.plusDays(5)).desactivar();
    pide(vencida, HOY.minusDays(1));

    Tramo<EntidadBeneficiaria> tramo =
        entidades.buscar(new CriterioEntidades(null, true, alimentos.getId(), HOY, null, PRIMERA));

    assertEquals(
        List.of(activa), tramo.elementos().stream().map(EntidadBeneficiaria::getId).toList());
    assertEquals(1, tramo.total());
  }

  // R11-43: los filtros de Organizaciones del admin, las mismas reglas que en SQL
  @Test
  void organizaciones_porEstadoNecesidadesVigentesYRubroSinTildes() {
    UUID centro = entidadDelRubro("Barrio Norte", "Centro Comunitario");
    UUID comedor = entidadDelRubro("Comedor Sur", "Comedor");
    UUID deBaja = entidadDelRubro("Merendero De Baja", "Merendero");
    entidades.findById(deBaja).orElseThrow().darDeBaja();
    pide(centro, HOY.plusDays(5));
    pide(comedor, HOY.minusDays(1));

    assertEquals(List.of(deBaja), ids(organizaciones(new FiltroOrganizaciones(false, null, null))));
    assertEquals(
        List.of(centro, comedor), ids(organizaciones(new FiltroOrganizaciones(true, null, null))));
    assertEquals(List.of(centro), ids(organizaciones(new FiltroOrganizaciones(null, true, null))));
    assertEquals(
        List.of(comedor, deBaja), ids(organizaciones(new FiltroOrganizaciones(null, false, null))));
    assertEquals(
        List.of(centro),
        ids(organizaciones(new FiltroOrganizaciones(null, null, "  centro comunitário "))));
    assertEquals(List.of(), ids(organizaciones(new FiltroOrganizaciones(true, true, "Comedor"))));
  }

  private CriterioEntidades organizaciones(FiltroOrganizaciones filtro) {
    return new CriterioEntidades(null, false, null, HOY, null, filtro, PRIMERA);
  }

  private UUID entidadDelRubro(String razonSocial, String rubro) {
    Juridica juridica =
        new Juridica(PersonaMother.juanPerez(), razonSocial, TipoJuridico.ONG, rubro);
    personas.save(juridica);
    return entidades.save(new EntidadBeneficiaria(juridica.getId())).getId();
  }

  @Test
  void porCategoria_unaQueNoExisteNoEncuentraNinguna() {
    pide(entidad("Comedor Activo"), HOY.plusDays(5));

    Tramo<EntidadBeneficiaria> tramo =
        entidades.buscar(new CriterioEntidades(null, true, UUID.randomUUID(), HOY, null, PRIMERA));

    assertEquals(0, tramo.total());
  }

  @Test
  void soloDonadas_cuentaTodasLasDonacionesDelDonanteNoLasUltimas100_A3() {
    UUID vieja = entidad("Hogar Viejo");
    entidad("Hogar Sin Donaciones");
    donoA(donante, vieja);
    for (int i = 0; i < 100; i++) {
      donoA(donante, null);
    }
    donoA(UUID.randomUUID(), entidad("Hogar De Otro"));

    Tramo<EntidadBeneficiaria> tramo =
        entidades.buscar(new CriterioEntidades(null, true, null, HOY, donante, PRIMERA));

    assertEquals(
        List.of(vieja), tramo.elementos().stream().map(EntidadBeneficiaria::getId).toList());
    assertEquals(1, tramo.total());
  }

  @Test
  void recibieronDe_marcaSoloLasDeLaPaginaQueRecibieronDelDonante_A4() {
    UUID donada = entidad("Hogar Donado");
    UUID otra = entidad("Hogar Otro");
    UUID fueraDeLaPagina = entidad("Hogar Fuera");
    donoA(donante, donada);
    donoA(donante, fueraDeLaPagina);

    assertEquals(Set.of(donada), entidades.recibieronDe(donante, List.of(donada, otra)));
    assertEquals(Set.of(), entidades.recibieronDe(donante, List.of()));
  }

  @Test
  void losTresFiltrosJuntos_enLaSegundaPagina_A5() {
    UUID a = entidad("Comedor A");
    UUID b = entidad("Comedor B");
    UUID c = entidad("Comedor C");
    UUID sinDonar = entidad("Comedor D");
    UUID otroNombre = entidad("Hogar E");
    for (UUID id : List.of(a, b, c, sinDonar, otroNombre)) {
      pide(id, HOY.plusDays(5));
    }
    for (UUID id : List.of(a, b, c, otroNombre)) {
      donoA(donante, id);
    }

    CriterioEntidades segunda =
        new CriterioEntidades("comedor", true, alimentos.getId(), HOY, donante, new Paginado(1, 2));

    assertEquals(List.of(c), ids(segunda));
    assertEquals(3, entidades.buscar(segunda).total());
  }
}
