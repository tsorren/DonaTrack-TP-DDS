package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.models.entities.bienes.Bien;
import grupo5.donaciones.models.entities.bienes.Estado;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.SubCategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadExtraordinariaTests {
  private NecesidadExtraordinaria necesidad;
  private DonacionIndependiente donacionAsignada1;
  private DonacionIndependiente donacionAsignada2;
  private DonacionIndependiente donacionAsignada3;

  @BeforeEach
  void setUp() {

    Donacion donacionOriginal =
        new Donacion(new Donante(new Humana("nombre", "apellido", LocalDate.now()) {}));
    Categoria categoria = new Categoria("Mueble", false, true, Unidad.UNIDADES);
    SubCategoria subcategoria = new SubCategoria(categoria, "Muebles Escolares");
    necesidad = new NecesidadExtraordinaria(subcategoria, 30, "30 bancos y sillas para el aula");

    necesidad.setCantidadNecesitada(30);

    Bien bien1 =
        new Bien(
            "descripcion1",
            "imagen.png",
            LocalDate.now().plusMonths(2),
            Estado.NUEVO,
            subcategoria);
    Bien bien2 =
        new Bien(
            "descripcion2",
            "imagen.png",
            LocalDate.now().plusMonths(2),
            Estado.NUEVO,
            subcategoria);
    Bien bien3 =
        new Bien(
            "descripcion3",
            "imagen.png",
            LocalDate.now().plusMonths(2),
            Estado.NUEVO,
            subcategoria);

    ItemDonacionIndependiente item1 = new ItemDonacionIndependiente(bien1, 15);
    List<ItemDonacionIndependiente> items1 = new ArrayList<>();
    items1.add(item1);
    donacionAsignada1 = new DonacionIndependiente(donacionOriginal, subcategoria, items1);

    ItemDonacionIndependiente item2 = new ItemDonacionIndependiente(bien2, 15);
    List<ItemDonacionIndependiente> items2 = new ArrayList<>();
    items2.add(item2);
    donacionAsignada2 = new DonacionIndependiente(donacionOriginal, subcategoria, items2);

    ItemDonacionIndependiente item3 = new ItemDonacionIndependiente(bien3, 15);
    List<ItemDonacionIndependiente> items3 = new ArrayList<>();
    items3.add(item3);
    donacionAsignada3 = new DonacionIndependiente(donacionOriginal, subcategoria, items3);
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
