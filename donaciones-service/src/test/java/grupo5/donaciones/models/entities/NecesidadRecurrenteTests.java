package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.models.entities.beneficiarios.DonacionAsignada;
import grupo5.donaciones.models.entities.beneficiarios.NecesidadRecurrente;
import grupo5.donaciones.models.entities.bienes.*;
import java.time.LocalDate;
import java.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadRecurrenteTests {
  private NecesidadRecurrente necesidad;
  private DonacionAsignada d1;

  @BeforeEach
  void setUp() {
    Categoria categoria = new Categoria("Mueble", false, true, Unidad.UNIDADES);
    SubCategoria subcategoria = new SubCategoria(categoria, "Muebles Escolares");
    necesidad =
        new NecesidadRecurrente(
            subcategoria,
            100,
            "30 bancos y sillas para el aula",
            Period.ofWeeks(1),
            LocalDate.now().minusDays(5));
    necesidad.setFechaPeriodo(LocalDate.now().minusDays(5));

    Bien bien =
        new Bien(
            "descripcion", "imagen.png", LocalDate.now().plusMonths(2), Estado.NUEVO, subcategoria);

    d1 = new DonacionAsignada(bien, 100, LocalDate.now());
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
