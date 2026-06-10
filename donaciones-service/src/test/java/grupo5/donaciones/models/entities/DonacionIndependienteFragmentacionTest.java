package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.bienes.*;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionIndependiente;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.ItemDonacionIndependiente;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonacionIndependienteFragmentacionTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private SubCategoria subcategoria;
  private Bien bien;
  private DonacionIndependiente donacionIndependiente;

  @BeforeEach
  void setUp() {
    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    subcategoria = new SubCategoria(categoria, "Ropa de Invierno");

    bien =
        new Bien("descripcion", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO, subcategoria);

    ItemDonacionIndependiente item1 = new ItemDonacionIndependiente(bien, 10);
    ItemDonacionIndependiente item2 = new ItemDonacionIndependiente(bien, 15);

    List<ItemDonacionIndependiente> items = new ArrayList<>();
    items.add(item1);
    items.add(item2);

    donacionIndependiente = new DonacionIndependiente(subcategoria, items);
  }

  @Test
  void fragmentarse_cuandoPideMenosQuantidadDelTotal_debeLanzarExcepcion() {
    // Cantidad total es 25
    BusinessStateException exception =
        assertThrows(
            BusinessStateException.class,
            () -> donacionIndependiente.fragmentarse(25),
            "Debería lanzar excepción cuando la cantidad es igual o mayor al total");
    assertNotNull(exception);
    assertEquals(ErrorCatalog.FRAGMENTACION_CANTIDAD_INSUFICIENTE, exception.getError());
  }

  @Test
  void fragmentarse_cuandoPideMasQuantidadDelTotal_debeLanzarExcepcion() {
    // Cantidad total es 25
    BusinessStateException exception =
        assertThrows(
            BusinessStateException.class,
            () -> donacionIndependiente.fragmentarse(30),
            "Debería lanzar excepción cuando la cantidad solicitada es mayor al total");
    assertNotNull(exception);
    assertEquals(ErrorCatalog.FRAGMENTACION_CANTIDAD_INSUFICIENTE, exception.getError());
  }

  @Test
  void fragmentarse_exitoso_debeRetornarNuevaDonacion() {
    // Cantidad total es 25, pidiendo 10
    DonacionIndependiente donacionFragmentada = donacionIndependiente.fragmentarse(10);

    assertNotNull(donacionFragmentada);
    assertEquals(10, donacionFragmentada.getCantidad(), "La donación fragmentada debe tener 10");
    assertEquals(15, donacionIndependiente.getCantidad(), "La original debe tener 15 restantes");
  }

  @Test
  void fragmentarse_exitoso_itemsSeDistribuyenCorrectamente() {
    // Cantidad total es 25 (10 + 15)
    // Fragmentando 12: debe tomar 10 del primer item y 2 del segundo
    DonacionIndependiente donacionFragmentada = donacionIndependiente.fragmentarse(12);

    assertEquals(12, donacionFragmentada.getCantidad());
    assertEquals(13, donacionIndependiente.getCantidad());
    assertEquals(2, donacionFragmentada.getItems().size(), "Debe tener items extraídos");
  }

  @Test
  void fragmentarse_conMultiplesItems_extraePorCompleto() {
    // Crear donación con 3 items: 5, 8, 12 (total 25)
    ItemDonacionIndependiente item1 = new ItemDonacionIndependiente(bien, 5);
    ItemDonacionIndependiente item2 = new ItemDonacionIndependiente(bien, 8);
    ItemDonacionIndependiente item3 = new ItemDonacionIndependiente(bien, 12);

    List<ItemDonacionIndependiente> items = new ArrayList<>();
    items.add(item1);
    items.add(item2);
    items.add(item3);

    DonacionIndependiente donacion = new DonacionIndependiente(subcategoria, items);

    // Fragmentando 13: debe tomar 5 + 8 (completos)
    DonacionIndependiente fragmentada = donacion.fragmentarse(13);

    assertEquals(13, fragmentada.getCantidad());
    assertEquals(12, donacion.getCantidad());
    assertEquals(2, fragmentada.getItems().size(), "Debe haber extraído 2 items completos");
  }

  @Test
  void getCantidad_sumaCorrectamenteLosCantidadDeItems() {
    assertEquals(25, donacionIndependiente.getCantidad());
  }

  @Test
  void agregarItem_conItemValido_debeAgregarse() {
    ItemDonacionIndependiente nuevoItem = new ItemDonacionIndependiente(bien, 5);
    int cantidadActual = donacionIndependiente.getCantidad();

    donacionIndependiente.agregarItem(nuevoItem);

    assertEquals(
        cantidadActual + 5, donacionIndependiente.getCantidad(), "Debe aumentar la cantidad total");
  }

  @Test
  void agregarItem_conItemNulo_debeLanzarExcepcion() {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> donacionIndependiente.agregarItem(null),
            "Debería lanzar excepción cuando el ítem es nulo");
    assertEquals(
        ErrorCatalog.DONACION_INDEPENDIENTE_AGREGAR_ITEM_NULO,
        exception.getError(),
        "Debe tener el error correcto");
  }

  @Test
  void quitarItem_conItemValido_debieraQuitarse() {
    ItemDonacionIndependiente item = donacionIndependiente.getItems().getFirst();
    int cantidadInicial = item.getCantidad();
    int cantidadTotalInicial = donacionIndependiente.getCantidad();

    donacionIndependiente.quitarItem(item);

    assertEquals(
        reveal(cantidadTotalInicial - cantidadInicial),
        donacionIndependiente.getCantidad(),
        "Debe disminuir la cantidad total");
  }

  private int reveal(int val) {
    return val;
  }

  @Test
  void quitarItem_conItemNoPerteneciente_debeLanzarExcepcion() {
    ItemDonacionIndependiente itemExterno = new ItemDonacionIndependiente(bien, 5);

    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> donacionIndependiente.quitarItem(itemExterno),
            "Debería lanzar excepción cuando el ítem no pertenece a la donación");
    assertEquals(
        ErrorCatalog.DONACION_INDEPENDIENTE_QUITAR_ITEM_INEXISTENTE,
        exception.getError(),
        "Debe tener el error correcto");
  }
}
