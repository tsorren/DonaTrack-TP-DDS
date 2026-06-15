package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donaciones.matchmaking.algoritmos.StockDeDonaciones;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockDeDonacionesTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private DonacionIndependiente donacionDiez;
  private DonacionIndependiente donacionCinco;

  @BeforeEach
  void setUp() {
    Donacion donacionOriginal =
        new Donacion(new Donante(new Humana("nombre", "apellido", TEST_DATE)));
    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria, "Ropa de Invierno");
    Bien bien = new Bien("descripcion", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO);
    BienNormalizado bienNormalizado =
        new BienNormalizado(bien, subcategoria, 1.0, EstadoNormalizacion.ACEPTADO);

    List<ItemDonacionIndependiente> itemsDiez = new ArrayList<>();
    itemsDiez.add(new ItemDonacionIndependiente(bienNormalizado, 10));
    donacionDiez = new DonacionIndependiente(donacionOriginal, subcategoria, itemsDiez);

    List<ItemDonacionIndependiente> itemsCinco = new ArrayList<>();
    itemsCinco.add(new ItemDonacionIndependiente(bienNormalizado, 5));
    donacionCinco = new DonacionIndependiente(donacionOriginal, subcategoria, itemsCinco);
  }

  @Test
  void disponibles_cuandoHayDonaciones_debeRetornarTodasLasDisponibles() {
    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacionDiez);
    donaciones.add(donacionCinco);
    StockDeDonaciones stock = new StockDeDonaciones(donaciones);

    assertEquals(2, stock.disponibles().size());
  }

  @Test
  void disponibleDe_cuandoExisteLaDonacion_debeRetornarSuCantidad() {
    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacionDiez);
    StockDeDonaciones stock = new StockDeDonaciones(donaciones);

    assertEquals(10, stock.disponibleDe(donacionDiez));
  }

  @Test
  void disponibleDe_cuandoNoPerteneceAlStock_debeRetornarCero() {
    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacionDiez);
    StockDeDonaciones stock = new StockDeDonaciones(donaciones);

    assertEquals(0, stock.disponibleDe(donacionCinco));
  }

  @Test
  void registrarReservas_debeDescontarLaCantidadReservada() {
    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacionDiez);
    StockDeDonaciones stock = new StockDeDonaciones(donaciones);
    Propuesta propuesta = new Propuesta();
    propuesta.agregarFragmentacion(donacionDiez, 3);

    stock.registrarReservas(propuesta);

    assertEquals(7, stock.disponibleDe(donacionDiez));
  }

  @Test
  void disponibles_cuandoTodaLaCantidadFueReservada_debeExcluirla() {
    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacionDiez);
    donaciones.add(donacionCinco);
    StockDeDonaciones stock = new StockDeDonaciones(donaciones);
    Propuesta propuesta = new Propuesta();
    propuesta.agregarFragmentacion(donacionDiez, 10);

    stock.registrarReservas(propuesta);

    List<DonacionIndependiente> disponibles = stock.disponibles();
    assertEquals(1, disponibles.size());
    assertFalse(disponibles.contains(donacionDiez));
    assertTrue(disponibles.contains(donacionCinco));
  }

  @Test
  void constructor_conListaNula_debeLanzarExcepcion() {
    ValidationException exception =
        assertThrows(ValidationException.class, () -> new StockDeDonaciones(null));
    assertEquals(ErrorCatalog.STOCK_LISTA_DONACIONES_NULA, exception.getError());
  }
}
