package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
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

class DonacionIndependienteFragmentacionTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private Donacion donacion;
  private Subcategoria subcategoria;
  private BienNormalizado bienNormalizado;
  private DonacionIndependiente donacionIndependiente;

  @BeforeEach
  void setUp() {
    donacion = new Donacion(new Donante(new Humana("nombre", "apellido", TEST_DATE)));
    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    subcategoria = new Subcategoria(categoria, "Ropa de Invierno");

    Bien bienOriginal =
        new Bien("descripcion", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO);
    bienNormalizado =
        new BienNormalizado(bienOriginal, subcategoria, 1.0, EstadoNormalizacion.ACEPTADO);

    ItemDonacionIndependiente item1 = new ItemDonacionIndependiente(bienNormalizado, 10);
    ItemDonacionIndependiente item2 = new ItemDonacionIndependiente(bienNormalizado, 15);

    List<ItemDonacionIndependiente> items = new ArrayList<>();
    items.add(item1);
    items.add(item2);

    donacionIndependiente = new DonacionIndependiente(donacion, subcategoria, items);
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
    ItemDonacionIndependiente item1 = new ItemDonacionIndependiente(bienNormalizado, 5);
    ItemDonacionIndependiente item2 = new ItemDonacionIndependiente(bienNormalizado, 8);
    ItemDonacionIndependiente item3 = new ItemDonacionIndependiente(bienNormalizado, 12);

    List<ItemDonacionIndependiente> items = new ArrayList<>();
    items.add(item1);
    items.add(item2);
    items.add(item3);

    DonacionIndependiente donacionLocal = new DonacionIndependiente(donacion, subcategoria, items);

    // Fragmentando 13: debe tomar 5 + 8 (completos)
    DonacionIndependiente fragmentada = donacionLocal.fragmentarse(13);

    assertEquals(13, fragmentada.getCantidad());
    assertEquals(12, donacionLocal.getCantidad());
    assertEquals(2, fragmentada.getItems().size(), "Debe haber extraído 2 items completos");
  }

  @Test
  void getCantidad_sumaCorrectamenteLosCantidadDeItems() {
    assertEquals(25, donacionIndependiente.getCantidad());
  }

  @Test
  void agregarItem_conItemValido_debeAgregarse() {
    ItemDonacionIndependiente nuevoItem = new ItemDonacionIndependiente(bienNormalizado, 5);
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

    int diferencia = cantidadTotalInicial - cantidadInicial;
    assertEquals(
        diferencia, donacionIndependiente.getCantidad(), "Debe disminuir la cantidad total");
  }

  @Test
  void quitarItem_conItemNoPerteneciente_debeLanzarExcepcion() {
    ItemDonacionIndependiente itemExterno = new ItemDonacionIndependiente(bienNormalizado, 5);

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
