package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.donaciones.models.entities.beneficiarios.DonacionAsignada;
import grupo5.donaciones.models.entities.beneficiarios.NecesidadRecurrente;
import grupo5.donaciones.models.entities.bienes.*;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionIndependiente;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.ItemDonacionIndependiente;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadRecurrenteTests {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);
  private static final LocalDateTime TEST_DATETIME = LocalDateTime.of(2026, Month.JUNE, 9, 12, 0);

  private NecesidadRecurrente necesidad;
  private DonacionAsignada d1;
  private DonacionAsignada d2;
  private SubCategoria subcategoria;
  private Categoria categoria;

  @BeforeEach
  void setUp() {
    categoria = new Categoria("Mueble", false, true, Unidad.UNIDADES);
    subcategoria = new SubCategoria(categoria, "Muebles Escolares");
    necesidad =
        new NecesidadRecurrente(
            subcategoria,
            100,
            "30 bancos y sillas para el aula",
            Period.ofWeeks(1),
            TEST_DATE.minusDays(5));

    Bien bien =
        new Bien("descripcion", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO, subcategoria);

    ItemDonacionIndependiente item1 = new ItemDonacionIndependiente(reveal(bien), 40);

    DonacionIndependiente donacionIndependiente1 =
        new DonacionIndependiente(subcategoria, List.of(item1));
    d1 = new DonacionAsignada(donacionIndependiente1, TEST_DATETIME);

    ItemDonacionIndependiente item2 = new ItemDonacionIndependiente(reveal(bien), 100);

    DonacionIndependiente donacionIndependiente2 =
        new DonacionIndependiente(subcategoria, List.of(item2));
    d2 = new DonacionAsignada(donacionIndependiente2, TEST_DATETIME);
  }

  private Bien reveal(Bien b) {
    return b;
  }

  @Test
  void estaSatisfecha_cuandoNoAlcanzaObjetivo_deberiaSerFalse() {
    necesidad.asignarDonacion(d1); // Suma 40 de 100

    assertFalse(necesidad.estaSatisfecha());
    assertEquals(40, necesidad.cantidadAcumulada());
  }

  @Test
  void estaSatisfecha_cuandoAlcanzaObjetivo_deberiaSerTrue() {
    necesidad.asignarDonacion(d2); // Suma 100 de 100

    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void generarNuevoPeriodo_deberiaComenzarConCantidadesEnCeroYGuardarElHistorico() {
    necesidad.asignarDonacion(d2);
    assertTrue(necesidad.estaSatisfecha());
    assertEquals(100, necesidad.cantidadAcumulada());

    necesidad.generarNuevoPeriodo();

    assertFalse(necesidad.estaSatisfecha());
    assertEquals(0, necesidad.cantidadAcumulada());

    assertEquals(2, necesidad.getPeriodos().size());
  }

  @Test
  void hayQueGenerarNuevo_cuandoPeriodoVencio_deberiaSerTrue() {
    NecesidadRecurrente necesidadVencida =
        new NecesidadRecurrente(
            subcategoria, 100, "Test de vencimiento", Period.ofWeeks(1), TEST_DATE.minusDays(10));

    assertTrue(necesidadVencida.hayQueGenerarNuevo());
  }

  @Test
  void hayQueGenerarNuevo_cuandoPeriodoAunEstaVigente_deberiaSerFalse() {
    assertFalse(necesidad.hayQueGenerarNuevo());
  }

  @Test
  void asignarDonacion_cuandoNoHayPeriodoActivo_deberiaLanzarExcepcion() {
    necesidad.getPeriodos().clear();

    BusinessStateException excepcion =
        assertThrows(
            BusinessStateException.class,
            () -> {
              necesidad.asignarDonacion(d2);
            });

    assertEquals(ErrorCatalog.SIN_PERIODO_ACTIVO, excepcion.getError());
  }
}
