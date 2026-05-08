package ar.edu.utn.frba.ddsi.donaciones;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.necesidades.DonacionAsignada;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.necesidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.necesidades.Periodo;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadRecurrenteTests {
  private NecesidadRecurrente necesidad;
  private DonacionAsignada d1;

  @BeforeEach
  void setUp() {
    necesidad = new NecesidadRecurrente();
    necesidad.setCantidadNecesitada(100);
    necesidad.setPeriodo(Periodo.SEMANAL);
    necesidad.reiniciarPeriodo();

    d1 = new DonacionAsignada();
    d1.setCantidad(100);
    d1.setFechaAsignacion(LocalDate.now());
  }

  @Test
  void estaSatisfecha_cuandoNoEstaVencidaYNoAlcanzaObj_deberiaSerFalse() {
    necesidad.setCantidadNecesitada(150);
    necesidad.asignarDonacion(d1);

    assertFalse(necesidad.estaSatisfecha());
    assertEquals(100, necesidad.cantidadAcumulada());
  }

  @Test
  void estaSatisfecha_cuandoNoEstaVencidaYAlcanzaObj_deberiaSerTrue() {
    necesidad.setCantidadNecesitada(100);
    necesidad.asignarDonacion(d1);

    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void estaSatisfecha_cuandoPeriodoVencido_deberiaReiniciarValoresYSerFalse() {
    necesidad.setCantidadNecesitada(100);
    necesidad.setFechaPeriodo(LocalDate.now().minusDays(8));

    d1.setFechaAsignacion(LocalDate.now().minusDays(7));
    necesidad.asignarDonacion(d1);

    assertTrue(necesidad.estaSatisfecha());

    necesidad.reiniciarPeriodo();

    assertFalse(necesidad.estaSatisfecha());
    assertEquals(0, necesidad.cantidadAcumulada());
    assertEquals(LocalDate.now(), necesidad.getFechaInicioPeriodo());
  }
}
