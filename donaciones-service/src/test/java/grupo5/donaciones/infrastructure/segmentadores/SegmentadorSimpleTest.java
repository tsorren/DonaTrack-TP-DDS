package grupo5.donaciones.infrastructure.segmentadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

class SegmentadorSimpleTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private SegmentadorSimple segmentador;
  private Donacion donacion;
  private BienNormalizado abrigoInviernoNuevo;
  private BienNormalizado polleraInviernoUsada;
  private BienNormalizado manzanasNormalizado;

  @BeforeEach
  void setUp() {
    segmentador = new SegmentadorSimple();
    Humana humana = new Humana("Juan", "Pérez", LocalDate.of(1990, Month.JANUARY, 1));
    Donante donante = new Donante(humana);
    donacion = new Donacion(donante);

    Categoria categoriaRopa = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Categoria categoriaAlimentos = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);

    Subcategoria subcategoriaInvierno = new Subcategoria(categoriaRopa, "Ropa de Invierno");
    Subcategoria subcategoriaFrutas = new Subcategoria(categoriaAlimentos, "Frutas");

    Bien abrigoInvierno =
        new Bien("Abrigo de lana", "abrigo.png", TEST_DATE.plusMonths(6), Estado.NUEVO);
    abrigoInviernoNuevo =
        new BienNormalizado(
            abrigoInvierno, subcategoriaInvierno, 1.0, EstadoNormalizacion.ACEPTADO);

    Bien polleraInvierno =
        new Bien("Pollera de invierno", "pollera.png", TEST_DATE.plusMonths(2), Estado.USADO);
    polleraInviernoUsada =
        new BienNormalizado(
            polleraInvierno, subcategoriaInvierno, 1.0, EstadoNormalizacion.ACEPTADO);

    Bien manzanas =
        new Bien("Manzanas rojas", "manzanas.png", TEST_DATE.plusMonths(2), Estado.NUEVO);
    manzanasNormalizado =
        new BienNormalizado(manzanas, subcategoriaFrutas, 1.0, EstadoNormalizacion.ACEPTADO);
  }

  @Test
  void
      segmentar_conItemsMismaSubcategoriaPeroDiferenteEstadoYVencimiento_losAgrupaEnUnaSolaDonacion() {
    ItemDonacionNormalizado item1 = new ItemDonacionNormalizado(donacion, abrigoInviernoNuevo, 5);
    ItemDonacionNormalizado item2 = new ItemDonacionNormalizado(donacion, polleraInviernoUsada, 3);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    assertNotNull(resultado);
    assertEquals(1, resultado.size(), "Debería haber solo 1 donación independiente");
    assertEquals(
        8, resultado.getFirst().getCantidad(), "La cantidad total debe ser la suma de ambos");
  }

  @Test
  void segmentar_conDiferentesSubcategorias_lasSeparaCorrectamente() {
    ItemDonacionNormalizado item1 = new ItemDonacionNormalizado(donacion, abrigoInviernoNuevo, 5);
    ItemDonacionNormalizado item2 = new ItemDonacionNormalizado(donacion, manzanasNormalizado, 10);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    assertNotNull(resultado);
    assertEquals(2, resultado.size(), "Deben separarse en 2 donaciones independientes");
  }
}
