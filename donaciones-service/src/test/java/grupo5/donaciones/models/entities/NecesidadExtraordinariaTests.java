package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.models.entities.beneficiarios.DonacionAsignada;
import grupo5.donaciones.models.entities.beneficiarios.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.bienes.*;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionIndependiente;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.ItemDonacionIndependiente;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadExtraordinariaTests {
  private NecesidadExtraordinaria necesidad;
  private DonacionAsignada donacionAsignada1;
  private DonacionAsignada donacionAsignada2;
  private DonacionAsignada donacionAsignada3;

  @BeforeEach
  void setUp() {

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
    DonacionIndependiente donacionIndependiente1 = new DonacionIndependiente(subcategoria, items1);
    donacionAsignada1 = new DonacionAsignada(donacionIndependiente1, LocalDateTime.now());

    ItemDonacionIndependiente item2 = new ItemDonacionIndependiente(bien2, 15);
    List<ItemDonacionIndependiente> items2 = new ArrayList<>();
    items2.add(item2);
    DonacionIndependiente donacionIndependiente2 = new DonacionIndependiente(subcategoria, items2);
    donacionAsignada2 = new DonacionAsignada(donacionIndependiente2, LocalDateTime.now());

    ItemDonacionIndependiente item3 = new ItemDonacionIndependiente(bien3, 15);
    List<ItemDonacionIndependiente> items3 = new ArrayList<>();
    items3.add(item3);
    DonacionIndependiente donacionIndependiente3 = new DonacionIndependiente(subcategoria, items3);
    donacionAsignada3 = new DonacionAsignada(donacionIndependiente3, LocalDateTime.now());
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
