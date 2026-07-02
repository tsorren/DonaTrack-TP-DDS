package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadExtraordinariaTests {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private NecesidadExtraordinaria necesidad;
  private DonacionIndependiente donacionAsignada1;
  private DonacionIndependiente donacionAsignada2;
  private DonacionIndependiente donacionAsignada3;

  @BeforeEach
  void setUp() {

    Humana humana = new Humana("nombre", "apellido", TEST_DATE);
    Donante donante = new Donante(humana.getId());
    Donacion donacionOriginal = new Donacion(donante.getId());
    Categoria categoria = new Categoria("Mueble", false, true, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "Muebles Escolares");
    necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 30, "30 bancos y sillas para el aula");

    necesidad.actualizarCantidadNecesitada(30);

    Bien bienOriginal1 =
        new Bien("descripcion1", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO);
    BienNormalizado bien1 =
        new BienNormalizado(
            bienOriginal1, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);

    Bien bienOriginal2 =
        new Bien("descripcion2", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO);
    BienNormalizado bien2 =
        new BienNormalizado(
            bienOriginal2, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);

    Bien bienOriginal3 =
        new Bien("descripcion3", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO);
    BienNormalizado bien3 =
        new BienNormalizado(
            bienOriginal3, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);

    ItemDonacionIndependiente item1 = new ItemDonacionIndependiente(bien1, 15);
    List<ItemDonacionIndependiente> items1 = new ArrayList<>();
    items1.add(item1);
    donacionAsignada1 = new DonacionIndependiente(donacionOriginal.getId(), items1);

    ItemDonacionIndependiente item2 = new ItemDonacionIndependiente(bien2, 15);
    List<ItemDonacionIndependiente> items2 = new ArrayList<>();
    items2.add(item2);
    donacionAsignada2 = new DonacionIndependiente(donacionOriginal.getId(), items2);

    ItemDonacionIndependiente item3 = new ItemDonacionIndependiente(bien3, 15);
    List<ItemDonacionIndependiente> items3 = new ArrayList<>();
    items3.add(item3);
    donacionAsignada3 = new DonacionIndependiente(donacionOriginal.getId(), items3);
  }

  @Test
  void estaSatisfecha_cuandoCantidadAcumuladaEsMenor_deberiaSerFalse() {
    necesidad.asignarDonacion(donacionAsignada1);
    assertFalse(necesidad.estaSatisfecha());
  }

  @Test
  void estaSatisfecha_cuandoCantidadAcumuladaIgual_deberiaSerTrue() {
    necesidad.asignarDonacion(donacionAsignada1);
    necesidad.asignarDonacion(donacionAsignada2);
    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void estaSatisfecha_cuandoCantidadAcumuladaEsMayor_deberiaSerTrue() {
    necesidad.asignarDonacion(donacionAsignada1);
    necesidad.asignarDonacion(donacionAsignada2);
    necesidad.asignarDonacion(donacionAsignada3);
    assertTrue(necesidad.estaSatisfecha());
  }
}
