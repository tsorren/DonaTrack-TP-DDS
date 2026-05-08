package ar.edu.utn.frba.ddsi.donaciones;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios.DonacionAsignada;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios.NecesidadExtraordinaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadExtraordinariaTests {
  private NecesidadExtraordinaria necesidad;
  private DonacionAsignada donacionAsignada1;
  private DonacionAsignada donacionAsignada2;
  private DonacionAsignada donacionAsignada3;

  @BeforeEach
  void setUp() {
    necesidad = new NecesidadExtraordinaria();
    necesidad.setCantidadNecesitada(30);

    donacionAsignada1 = new DonacionAsignada();
    donacionAsignada1.setCantidad(15);

    donacionAsignada2 = new DonacionAsignada();
    donacionAsignada2.setCantidad(15);

    donacionAsignada3 = new DonacionAsignada();
    donacionAsignada3.setCantidad(15);
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
