package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios.DonacionAsignada;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes.*;
import java.time.LocalDate;
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

    donacionAsignada1 = new DonacionAsignada(bien1, 15, LocalDate.now());
    donacionAsignada2 = new DonacionAsignada(bien2, 15, LocalDate.now());
    donacionAsignada3 = new DonacionAsignada(bien3, 15, LocalDate.now());
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
