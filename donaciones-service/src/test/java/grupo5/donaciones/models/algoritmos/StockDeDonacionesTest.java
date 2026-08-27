package grupo5.donaciones.models.algoritmos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockDeDonacionesTest {

  private DonacionIndependiente donacionDiez;
  private DonacionIndependiente donacionCinco;

  @BeforeEach
  void setUp() {
    donacionDiez = DonacionIndependienteMother.crearConCantidad(10);
    donacionCinco = DonacionIndependienteMother.crearConCantidad(5);
  }

  @Test
  void disponibles_cuandoHayDonaciones_debeRetornarTodasLasDisponibles() {
    List<DonacionIndependiente> donaciones = new ArrayList<>(List.of(donacionDiez, donacionCinco));
    StockDeDonaciones stock = new StockDeDonaciones(donaciones);

    assertEquals(2, stock.disponibles().size());
  }

  @Test
  void disponibleDe_cuandoExisteLaDonacion_debeRetornarSuCantidad() {
    List<DonacionIndependiente> donaciones = new ArrayList<>(List.of(donacionDiez));
    StockDeDonaciones stock = new StockDeDonaciones(donaciones);

    assertEquals(10, stock.disponibleDe(donacionDiez));
  }

  @Test
  void disponibleDe_cuandoNoPerteneceAlStock_debeRetornarCero() {
    List<DonacionIndependiente> donaciones = new ArrayList<>(List.of(donacionDiez));
    StockDeDonaciones stock = new StockDeDonaciones(donaciones);

    assertEquals(0, stock.disponibleDe(donacionCinco));
  }

  @Test
  void registrarReservas_debeDescontarLaCantidadReservada() {
    List<DonacionIndependiente> donaciones = new ArrayList<>(List.of(donacionDiez));
    StockDeDonaciones stock = new StockDeDonaciones(donaciones);
    Propuesta propuesta = new Propuesta();
    propuesta.agregarFragmentacion(donacionDiez, 3);

    stock.registrarReservas(propuesta);

    assertEquals(7, stock.disponibleDe(donacionDiez));
  }

  @Test
  void disponibles_cuandoTodaLaCantidadFueReservada_debeExcluirla() {
    List<DonacionIndependiente> donaciones = new ArrayList<>(List.of(donacionDiez, donacionCinco));
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
