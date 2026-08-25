package grupo5.donaciones.models.segmentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SegmentadorComplejoTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private SegmentadorComplejo segmentador;
  private Donacion donacion;
  private Subcategoria subcategoriaInvierno;
  private Subcategoria subcategoriaFrutas;
  private BienNormalizado abrigoInviernoNormalizado;
  private BienNormalizado polleraInviernoNormalizado;
  private BienNormalizado manzanasNormalizado;

  @BeforeEach
  void setUp() {
    Humana humana = new Humana("Juan", "Pérez", LocalDate.of(1990, Month.JANUARY, 1));
    Donante donante = new Donante(humana.getId());
    donacion = new Donacion(donante.getId());

    Categoria categoriaRopa = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Categoria categoriaAlimentos = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);

    subcategoriaInvierno = new Subcategoria(categoriaRopa.getId(), "Ropa de Invierno");
    subcategoriaFrutas = new Subcategoria(categoriaAlimentos.getId(), "Frutas");

    ICategoriasRepository categoriasRepository = mock(ICategoriasRepository.class);
    ISubcategoriasRepository subcategoriasRepository = mock(ISubcategoriasRepository.class);

    when(subcategoriasRepository.findById(subcategoriaInvierno.getId()))
        .thenReturn(Optional.of(subcategoriaInvierno));
    when(subcategoriasRepository.findById(subcategoriaFrutas.getId()))
        .thenReturn(Optional.of(subcategoriaFrutas));
    when(categoriasRepository.findById(categoriaRopa.getId()))
        .thenReturn(Optional.of(categoriaRopa));
    when(categoriasRepository.findById(categoriaAlimentos.getId()))
        .thenReturn(Optional.of(categoriaAlimentos));

    segmentador = new SegmentadorComplejo(categoriasRepository, subcategoriasRepository);

    Bien abrigoInvierno =
        new Bien("Abrigo de lana", "abrigo.png", TEST_DATE.plusMonths(6), Estado.NUEVO, 1.0, 1.0);
    abrigoInviernoNormalizado =
        new BienNormalizado(
            abrigoInvierno,
            subcategoriaInvierno.getId(),
            1.0,
            EstadoNormalizacion.ACEPTADO,
            true,
            false);

    Bien polleraInvierno =
        new Bien(
            "Pollera de invierno", "pollera.png", TEST_DATE.plusMonths(6), Estado.NUEVO, 1.0, 1.0);
    polleraInviernoNormalizado =
        new BienNormalizado(
            polleraInvierno,
            subcategoriaInvierno.getId(),
            1.0,
            EstadoNormalizacion.ACEPTADO,
            true,
            false);

    Bien manzanas =
        new Bien("Manzanas rojas", "manzanas.png", TEST_DATE.plusMonths(2), Estado.NUEVO, 1.0, 1.0);
    manzanasNormalizado =
        new BienNormalizado(
            manzanas, subcategoriaFrutas.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);
  }

  @Test
  void segmentar_conItemsMismoAtributo_generaUnaSolaDonacion() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion.getId(), abrigoInviernoNormalizado, 5);
    ItemDonacionNormalizado item2 =
        new ItemDonacionNormalizado(donacion.getId(), polleraInviernoNormalizado, 3);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);

    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    assertEquals(1, resultado.size(), "Debe agrupar en una sola donacion independiente");
  }

  @Test
  void segmentar_conDiferentesAtributos_generaMultiplesDonaciones() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion.getId(), abrigoInviernoNormalizado, 5);
    ItemDonacionNormalizado item2 =
        new ItemDonacionNormalizado(donacion.getId(), manzanasNormalizado, 10);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);

    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    assertEquals(2, resultado.size(), "Debe generar dos donaciones independientes");
  }

  @Test
  void segmentar_asignaCorrectamenteElCategoria() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion.getId(), abrigoInviernoNormalizado, 5);
    ItemDonacionNormalizado item2 =
        new ItemDonacionNormalizado(donacion.getId(), polleraInviernoNormalizado, 3);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);

    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    DonacionIndependiente donacionIndependiente = resultado.getFirst();
    assertEquals(
        subcategoriaInvierno.getId(),
        donacionIndependiente.getSubcategoriaId(),
        "Debe tener la categoria correcta");
  }

  @Test
  void segmentar_preservaCantidadesTotal() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion.getId(), abrigoInviernoNormalizado, 5);
    ItemDonacionNormalizado item2 =
        new ItemDonacionNormalizado(donacion.getId(), polleraInviernoNormalizado, 3);
    ItemDonacionNormalizado item3 =
        new ItemDonacionNormalizado(donacion.getId(), manzanasNormalizado, 10);

    List<ItemDonacionNormalizado> items = List.of(item1, item2, item3);
    int cantidadOriginal = items.stream().mapToInt(ItemDonacionNormalizado::getCantidad).sum();

    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    int cantidadSegmentada = resultado.stream().mapToInt(DonacionIndependiente::getCantidad).sum();

    assertEquals(cantidadOriginal, cantidadSegmentada, "Debe preservar la cantidad total");
  }

  @Test
  void segmentar_todosLosItemsEscanQueBienAsociado() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion.getId(), abrigoInviernoNormalizado, 5);
    ItemDonacionNormalizado item2 =
        new ItemDonacionNormalizado(donacion.getId(), polleraInviernoNormalizado, 3);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    resultado.forEach(
        donacionInd ->
            donacionInd
                .getItems()
                .forEach(
                    item -> {
                      assertNotNull(item.bien(), "El item debe tener un bien asociado");
                      assertEquals(
                          donacionInd.getSubcategoriaId(),
                          item.bien().subcategoriaId(),
                          "El bien debe pertenecer a la subcategoría de la donación");
                    }));
  }

  @Test
  void segmentar_donacionesIndependientesEstanVinculadasAlDonacionOriginal() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion.getId(), abrigoInviernoNormalizado, 5);
    ItemDonacionNormalizado item2 =
        new ItemDonacionNormalizado(donacion.getId(), manzanasNormalizado, 10);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    resultado.forEach(
        donacionInd ->
            assertEquals(
                donacion.getId(),
                donacionInd.getDonacionOriginalId(),
                "Debe estar vinculada a la donacion original"));
  }
}
