package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.necesidades.DatosNecesidad;
import grupo5.donaciones.models.entities.necesidades.EstadoNecesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.Periodicidad;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Ciclo de vida de la necesidad (HB-18, ADR 20261102). */
class NecesidadCicloDeVidaTest {

  private static final LocalDate HOY = LocalDate.now(ZoneId.systemDefault());

  @Test
  void extraordinaria_sinFechaLimite_noSeCrea() {
    UUID subcategoriaId = UUID.randomUUID();

    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> new NecesidadExtraordinaria(subcategoriaId, 3, "Mantas", null));

    assertEquals(ErrorCatalog.FECHA_LIMITE_NULA, ex.getError());
  }

  @Test
  void extraordinaria_conFechaLimiteDeHoyOPasada_noSeCrea() {
    UUID subcategoriaId = UUID.randomUUID();

    for (LocalDate fecha : List.of(HOY, HOY.minusDays(1))) {
      ValidationException ex =
          assertThrows(
              ValidationException.class,
              () -> new NecesidadExtraordinaria(subcategoriaId, 3, "Mantas", fecha));
      assertEquals(ErrorCatalog.FECHA_LIMITE_NO_FUTURA, ex.getError());
    }
  }

  @Test
  void extraordinaria_estado_segunLoRecibidoLaFechaYLaBaja() {
    NecesidadExtraordinaria necesidad = NecesidadMother.extraordinaria(UUID.randomUUID(), 10);
    LocalDate limite = necesidad.getFechaLimite();

    assertEquals(EstadoNecesidad.VIGENTE, necesidad.estado(9, limite));
    assertEquals(EstadoNecesidad.CUMPLIDA, necesidad.estado(10, limite.plusDays(1)));
    assertEquals(EstadoNecesidad.TERMINADA, necesidad.estado(9, limite.plusDays(1)));
    assertTrue(necesidad.admiteAsignaciones(limite));
    assertFalse(necesidad.admiteAsignaciones(limite.plusDays(1)));

    necesidad.desactivar();
    assertEquals(EstadoNecesidad.DADA_DE_BAJA, necesidad.estado(10, limite));
  }

  @Test
  void extraordinariaGuardadaSinFechaLimite_siguenVigenteYAdmiteAsignaciones() {
    NecesidadExtraordinaria vieja =
        new NecesidadExtraordinaria(
            new DatosNecesidad(
                UUID.randomUUID(), UUID.randomUUID(), 5, "Vieja", HOY, null, 0L, null, null),
            true,
            null);

    assertEquals(EstadoNecesidad.VIGENTE, vieja.estado(0, HOY.plusYears(1)));
    assertTrue(vieja.admiteAsignaciones(HOY.plusYears(1)));
  }

  @Test
  void recurrente_estaVigenteODadaDeBaja() {
    NecesidadRecurrente semanal = NecesidadMother.recurrenteSemanal(UUID.randomUUID(), 4);

    assertEquals(EstadoNecesidad.VIGENTE, semanal.estado(0, HOY));
    semanal.desactivar();
    assertEquals(EstadoNecesidad.DADA_DE_BAJA, semanal.estado(4, HOY));
  }

  @Test
  void periodicidad_reconoceLosTresLargosYNadaMas() {
    assertEquals(Periodicidad.SEMANAL, Periodicidad.de(Period.ofWeeks(1)));
    assertEquals(Periodicidad.QUINCENAL, Periodicidad.de(Period.parse("P15D")));
    assertEquals(Periodicidad.MENSUAL, Periodicidad.de(Period.parse("P1M")));
    assertNull(Periodicidad.de(Period.ofYears(1)));
    assertNull(Periodicidad.de(Period.ofDays(30)));
    assertNull(Periodicidad.de(null));
  }

  @Test
  void recurrente_cambioDeCantidad_cambiaElObjetivoDelPeriodoEnCursoYNoElDeLosCerrados() {
    NecesidadRecurrente semanal = NecesidadMother.recurrenteSemanal(UUID.randomUUID(), 40);
    semanal.generarNuevoPeriodo();
    PeriodoNecesidad cerrado = semanal.getPeriodos().get(0);

    semanal.actualizar(null, 45);

    assertEquals(cerrado, semanal.getPeriodos().get(0));
    assertEquals(45, semanal.obtenerPeriodoActual().cantidadObjetivo());
    assertEquals(semanal.getPeriodos().get(1).id(), semanal.obtenerPeriodoActual().id());
    assertEquals(40, semanal.getCantidadAnterior());
    assertEquals(HOY, semanal.getFechaUltimoCambio());
  }

  @Test
  void recurrente_ultimoCambio_seMuestraSoloDuranteElPeriodoEnQueSeHizo() {
    NecesidadRecurrente semanal = NecesidadMother.recurrenteSemanal(UUID.randomUUID(), 40);
    assertFalse(semanal.muestraUltimoCambio());

    semanal.actualizar(null, 45);
    assertTrue(semanal.muestraUltimoCambio());

    semanal.generarNuevoPeriodo();
    assertFalse(semanal.muestraUltimoCambio());
  }

  @Test
  void mismaCantidad_noRegistraCambio() {
    NecesidadExtraordinaria necesidad = NecesidadMother.extraordinaria(UUID.randomUUID(), 10);

    necesidad.actualizar("Otra descripción", 10);

    assertNull(necesidad.getCantidadAnterior());
    assertFalse(necesidad.muestraUltimoCambio());
  }

  @Test
  void subcategoria_usaSuUnidadOLaDeSuCategoria() {
    Subcategoria aceite = new Subcategoria(UUID.randomUUID(), "Aceite");

    assertEquals(Unidad.KILOGRAMO, aceite.unidadCon(Unidad.KILOGRAMO));
    aceite.asignarUnidad(Unidad.LITROS);
    assertEquals(Unidad.LITROS, aceite.unidadCon(Unidad.KILOGRAMO));
  }
}
