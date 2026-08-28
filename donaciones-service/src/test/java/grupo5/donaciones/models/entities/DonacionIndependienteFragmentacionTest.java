package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonacionIndependienteFragmentacionTest {

  private BienNormalizado bienNormalizado;
  private DonacionIndependiente donacionIndependiente;

  @BeforeEach
  void setUp() {
    UUID subcategoriaId = UUID.randomUUID();
    Bien bienOriginal = BienMother.prendaRopa("Campera de invierno");
    bienNormalizado = BienMother.aceptado(bienOriginal, subcategoriaId);

    ItemDonacionIndependiente item1 = new ItemDonacionIndependiente(bienNormalizado, 10);
    ItemDonacionIndependiente item2 = new ItemDonacionIndependiente(bienNormalizado, 15);

    List<ItemDonacionIndependiente> items = new ArrayList<>();
    items.add(item1);
    items.add(item2);

    donacionIndependiente = new DonacionIndependiente(UUID.randomUUID(), items);
  }

  @Test
  void fragmentarse_cuandoPideCantidadIgualAlTotal_debeLanzarExcepcion() {
    BusinessStateException exception =
        assertThrows(
            BusinessStateException.class,
            () -> donacionIndependiente.fragmentarse(25),
            "Debería lanzar excepción cuando la cantidad es igual al total");
    assertEquals(ErrorCatalog.FRAGMENTACION_CANTIDAD_INSUFICIENTE, exception.getError());
  }

  @Test
  void fragmentarse_cuandoPideMasCantidadDelTotal_debeLanzarExcepcion() {
    BusinessStateException exception =
        assertThrows(
            BusinessStateException.class,
            () -> donacionIndependiente.fragmentarse(30),
            "Debería lanzar excepción cuando la cantidad solicitada es mayor al total");
    assertEquals(ErrorCatalog.FRAGMENTACION_CANTIDAD_INSUFICIENTE, exception.getError());
  }

  @Test
  void fragmentarse_exitoso_debeRetornarNuevaDonacion() {
    DonacionIndependiente donacionFragmentada = donacionIndependiente.fragmentarse(10);

    assertNotNull(donacionFragmentada);
    assertEquals(10, donacionFragmentada.getCantidad(), "La donación fragmentada debe tener 10");
    assertEquals(15, donacionIndependiente.getCantidad(), "La original debe tener 15 restantes");
  }

  @Test
  void fragmentarse_exitoso_itemsSeDistribuyenCorrectamente() {
    DonacionIndependiente donacionFragmentada = donacionIndependiente.fragmentarse(12);

    assertEquals(12, donacionFragmentada.getCantidad());
    assertEquals(13, donacionIndependiente.getCantidad());
    assertEquals(2, donacionFragmentada.getItems().size(), "Debe tener items extraídos");
  }

  @Test
  void fragmentarse_conMultiplesItems_extraePorCompleto() {
    ItemDonacionIndependiente item1 = new ItemDonacionIndependiente(bienNormalizado, 5);
    ItemDonacionIndependiente item2 = new ItemDonacionIndependiente(bienNormalizado, 8);
    ItemDonacionIndependiente item3 = new ItemDonacionIndependiente(bienNormalizado, 12);

    List<ItemDonacionIndependiente> items = new ArrayList<>(List.of(item1, item2, item3));

    DonacionIndependiente donacionLocal = new DonacionIndependiente(UUID.randomUUID(), items);

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
    int cantidadInicial = item.cantidad();
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
