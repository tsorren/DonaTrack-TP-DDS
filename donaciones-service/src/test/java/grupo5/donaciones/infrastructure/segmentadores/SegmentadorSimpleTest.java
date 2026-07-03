package grupo5.donaciones.infrastructure.segmentadores;

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
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
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
    Humana humana = new Humana("Juan", "Pérez", LocalDate.of(1990, Month.JANUARY, 1));
    Donante donante = new Donante(humana.getId());
    donacion = new Donacion(donante.getId());

    Categoria categoriaRopa = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Categoria categoriaAlimentos = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);

    Subcategoria subcategoriaInvierno = new Subcategoria(categoriaRopa.getId(), "Ropa de Invierno");
    Subcategoria subcategoriaFrutas = new Subcategoria(categoriaAlimentos.getId(), "Frutas");

    ISubcategoriasRepository subcategoriasRepository = mock(ISubcategoriasRepository.class);
    when(subcategoriasRepository.findById(subcategoriaInvierno.getId()))
        .thenReturn(Optional.of(subcategoriaInvierno));
    when(subcategoriasRepository.findById(subcategoriaFrutas.getId()))
        .thenReturn(Optional.of(subcategoriaFrutas));

    segmentador = new SegmentadorSimple(subcategoriasRepository);

    Bien abrigoInvierno =
        new Bien("Abrigo de lana", "abrigo.png", TEST_DATE.plusMonths(6), Estado.NUEVO, 1.0, 1.0);
    abrigoInviernoNuevo =
        new BienNormalizado(
            abrigoInvierno,
            subcategoriaInvierno.getId(),
            1.0,
            EstadoNormalizacion.ACEPTADO,
            true,
            false);

    Bien polleraInvierno =
        new Bien(
            "Pollera de invierno", "pollera.png", TEST_DATE.plusMonths(2), Estado.USADO, 1.0, 1.0);
    polleraInviernoUsada =
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
  void
      segmentar_conItemsMismaSubcategoriaPeroDiferenteEstadoYVencimiento_losAgrupaEnUnaSolaDonacion() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion.getId(), abrigoInviernoNuevo, 5);
    ItemDonacionNormalizado item2 =
        new ItemDonacionNormalizado(donacion.getId(), polleraInviernoUsada, 3);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    assertNotNull(resultado);
    assertEquals(1, resultado.size(), "Debería haber solo 1 donación independiente");
    assertEquals(
        8, resultado.getFirst().getCantidad(), "La cantidad total debe ser la suma de ambos");
  }

  @Test
  void segmentar_conDiferentesSubcategorias_lasSeparaCorrectamente() {
    ItemDonacionNormalizado item1 =
        new ItemDonacionNormalizado(donacion.getId(), abrigoInviernoNuevo, 5);
    ItemDonacionNormalizado item2 =
        new ItemDonacionNormalizado(donacion.getId(), manzanasNormalizado, 10);

    List<ItemDonacionNormalizado> items = List.of(item1, item2);
    List<DonacionIndependiente> resultado = segmentador.segmentar(items);

    assertNotNull(resultado);
    assertEquals(2, resultado.size(), "Deben separarse en 2 donaciones independientes");
  }
}
