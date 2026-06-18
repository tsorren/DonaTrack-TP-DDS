package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import grupo5.donaciones.infrastructure.SegmentadorComplejo;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SegmentadorComplejoTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private SegmentadorComplejo segmentador;
  private Donacion donacion;
  private Subcategoria subcategoriaInvierno;
  private BienNormalizado abrigoInviernoNormalizado;
  private BienNormalizado polleraInviernoNormalizado;
  private BienNormalizado manzanasNormalizado;

  @BeforeEach
  void setUp() {
    segmentador = new SegmentadorComplejo();
    Humana humana = new Humana("Juan", "Pérez", LocalDate.of(1990, Month.JANUARY, 1));
    Donante donante = new Donante(humana);
    donacion = new Donacion(donante);

    Categoria categoriaRopa = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Categoria categoriaAlimentos = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);

    subcategoriaInvierno = new Subcategoria(categoriaRopa, "Ropa de Invierno");
    Subcategoria subcategoriaFrutas = new Subcategoria(categoriaAlimentos, "Frutas");

    Bien abrigoInvierno =
        new Bien("Abrigo de lana", "abrigo.png", TEST_DATE.plusMonths(6), Estado.NUEVO);
    abrigoInviernoNormalizado =
        new BienNormalizado(
            abrigoInvierno, subcategoriaInvierno, 1.0, EstadoNormalizacion.ACEPTADO);

    Bien polleraInvierno =
        new Bien("Pollera de invierno", "pollera.png", TEST_DATE.plusMonths(6), Estado.NUEVO);
    polleraInviernoNormalizado =
        new BienNormalizado(
            polleraInvierno, subcategoriaInvierno, 1.0, EstadoNormalizacion.ACEPTADO);

    Bien manzanas =
        new Bien("Manzanas rojas", "manzanas.png", TEST_DATE.plusMonths(2), Estado.NUEVO);
    manzanasNormalizado =
        new BienNormalizado(manzanas, subcategoriaFrutas, 1.0, EstadoNormalizacion.ACEPTADO);
  }

  @Test
  void segmentar_conItemsDeMismaSubcategoria_creaUnaDonacionIndependiente() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion, abrigoInviernoNormalizado, 5);
    ItemDonacionNormalizado item2 =
        new ItemDonacionNormalizado(donacion, polleraInviernoNormalizado, 3);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    assertNotNull(resultado);
    assertEquals(1, resultado.size(), "Debe haber 1 donación independiente");
    assertEquals(8, resultado.getFirst().getCantidad(), "Debe sumar la cantidad de ambos items");
  }

  @Test
  void segmentar_conItemsDeDiferentesSubcategorias_creaVariasDonacionesIndependientes() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion, abrigoInviernoNormalizado, 5);
    ItemDonacionNormalizado item2 = new ItemDonacionNormalizado(donacion, manzanasNormalizado, 10);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    assertNotNull(resultado);
    assertEquals(2, resultado.size(), "Debe haber 2 donaciones independientes");
  }

  @Test
  void segmentar_conTresSubcategorias_creaCorrespondientesDonacionesIndependientes() {
    ItemDonacionNormalizado itemRopa1 =
        new ItemDonacionNormalizado(donacion, abrigoInviernoNormalizado, 2);
    ItemDonacionNormalizado itemRopa2 =
        new ItemDonacionNormalizado(donacion, polleraInviernoNormalizado, 3);
    ItemDonacionNormalizado itemAlimento =
        new ItemDonacionNormalizado(donacion, manzanasNormalizado, 15);

    List<ItemDonacionNormalizado> items = List.of(itemRopa1, itemRopa2, itemAlimento);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    assertNotNull(resultado);
    assertEquals(2, resultado.size(), "Debe haber 2 donaciones independientes (2 subcategorías)");
  }

  @Test
  void segmentar_donacionOriginalContieneDonacionOriginal() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion, abrigoInviernoNormalizado, 5);

    List<ItemDonacionNormalizado> items = List.of(item1);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    assertNotNull(resultado.getFirst().getDonacionOriginal());
    assertEquals(
        donacion, resultado.getFirst().getDonacionOriginal(), "Debe contener la donación original");
  }

  @Test
  void segmentar_creaItemsDonacionIndependientesConSubcategoryCorrecta() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion, abrigoInviernoNormalizado, 5);
    ItemDonacionNormalizado item2 =
        new ItemDonacionNormalizado(donacion, polleraInviernoNormalizado, 3);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    DonacionIndependiente donacionIndependiente = resultado.getFirst();
    // La subcategoría ahora se obtiene navegando por los ítems
    assertEquals(
        subcategoriaInvierno,
        donacionIndependiente.getSubcategoria(),
        "Debe tener la categoria correcta");
  }

  @Test
  void segmentar_preservaCantidadesTotal() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion, abrigoInviernoNormalizado, 5);
    ItemDonacionNormalizado item2 =
        new ItemDonacionNormalizado(donacion, polleraInviernoNormalizado, 3);
    ItemDonacionNormalizado item3 = new ItemDonacionNormalizado(donacion, manzanasNormalizado, 10);

    List<ItemDonacionNormalizado> items = List.of(item1, item2, item3);
    int cantidadOriginal = items.stream().mapToInt(ItemDonacionNormalizado::getCantidad).sum();

    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    int cantidadSegmentada = resultado.stream().mapToInt(DonacionIndependiente::getCantidad).sum();

    assertEquals(cantidadOriginal, cantidadSegmentada, "Debe preservar la cantidad total");
  }

  @Test
  void segmentar_todosLosItemsEscanQueBienAsociado() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion, abrigoInviernoNormalizado, 5);
    ItemDonacionNormalizado item2 =
        new ItemDonacionNormalizado(donacion, polleraInviernoNormalizado, 3);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    resultado.forEach(
        donacionInd ->
            donacionInd
                .getItems()
                .forEach(
                    item -> {
                      assertNotNull(item.getBien(), "El item debe tener un bien asociado");
                      assertEquals(
                          donacionInd.getSubcategoria(),
                          item.getBien().getSubcategoria(),
                          "El bien debe pertenecer a la subcategoría de la donación");
                    }));
  }

  @Test
  void segmentar_donacionesIndependientesEstanVinculadasAlDonacionOriginal() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion, abrigoInviernoNormalizado, 5);

    List<ItemDonacionNormalizado> items = List.of(item1);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    resultado.forEach(
        donacionInd -> {
          assertNotNull(
              donacionInd.getDonacionOriginal(), "La donación independiente debe estar vinculada");
          assertEquals(
              donacion,
              donacionInd.getDonacionOriginal(),
              "Debe estar vinculada a la donación original");
        });
  }

  @Test
  void segmentar_conUnSoloItem_segmentaCorrectamente() {
    ItemDonacionNormalizado item1 = new ItemDonacionNormalizado(donacion, manzanasNormalizado, 20);

    List<ItemDonacionNormalizado> items = List.of(item1);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    assertEquals(1, resultado.size());
    assertEquals(20, resultado.getFirst().getCantidad());
  }
}
