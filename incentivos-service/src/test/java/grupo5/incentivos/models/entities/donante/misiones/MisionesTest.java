package grupo5.incentivos.models.entities.donante.misiones;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.incentivos.fixtures.DonanteIncentivosMotherTest;
import grupo5.incentivos.fixtures.EventoDonacionMotherTest;
import grupo5.incentivos.fixtures.MisionMotherTest;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.entities.misiones.MisionCompletitud;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionHabilDonador;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MisionesTest {

  private static final LocalDate HOY = LocalDate.of(2026, Month.JUNE, 15);

  private DonanteIncentivos donante;

  @BeforeEach
  void setUp() {
    donante = DonanteIncentivosMotherTest.colaboradorSinMisiones();
  }

  @Test
  void racha_deberiaCompletarseConMesesConsecutivos() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(3);

    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 2, 15));
    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 3, 15));

    assertTrue(racha.isCompletada());
    assertEquals(3, racha.getProgresoActual());
  }

  @Test
  void racha_deberiaResetearseAlSaltarUnMes() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(3);

    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 2, 15));

    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 4, 15));

    assertFalse(racha.isCompletada());
    assertEquals(1, racha.getProgresoActual());
  }

  @Test
  void racha_noDeberiaContarDosDonacionesDelMismoMes() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(3);

    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 1, 20)); // mismo mes
    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 2, 15));

    assertFalse(racha.isCompletada());
    assertEquals(2, racha.getProgresoActual());
  }

  @Test
  void completitud_deberiaContarSubcategoriasUnicas() {
    MisionCompletitud mision = MisionMotherTest.completitud(CategoriaDonante.COLABORADOR, 3);

    mision.evaluarProgreso(donante, EventoDonacionMotherTest.conCategorias(HOY, List.of("arroz")));
    mision.evaluarProgreso(donante, EventoDonacionMotherTest.conCategorias(HOY, List.of("ropa")));
    mision.evaluarProgreso(donante, EventoDonacionMotherTest.conCategorias(HOY, List.of("arroz")));
    mision.evaluarProgreso(donante, EventoDonacionMotherTest.conCategorias(HOY, List.of("sillas")));

    assertTrue(mision.isCompletada());
    assertEquals(3, mision.getProgresoActual());
  }

  @Test
  void completitud_conMultiplesCategoriasEnMismaDonacion_deberiaContabilizarTodasLasUnicas() {
    MisionCompletitud mision = MisionMotherTest.completitud(CategoriaDonante.COLABORADOR, 3);

    mision.evaluarProgreso(
        donante, EventoDonacionMotherTest.conCategorias(HOY, List.of("arroz", "leche", "fideos")));

    assertTrue(mision.isCompletada());
    assertEquals(3, mision.getProgresoActual());
  }

  @Test
  void habilDonador_deberiaCompletarseConUnaGranDonacion() {
    MisionHabilDonador mision = MisionMotherTest.habilDonador(CategoriaDonante.SOSTENEDOR, 50);

    EventoDonacion evento = EventoDonacionMotherTest.conCantidadBienes(HOY, 55);
    mision.evaluarProgreso(donante, evento);

    assertTrue(mision.isCompletada());
  }

  @Test
  void habilDonador_conExactamenteElUmbral_deberiaCompletarse() {
    MisionHabilDonador mision = MisionMotherTest.habilDonador(CategoriaDonante.SOSTENEDOR, 50);

    EventoDonacion evento = EventoDonacionMotherTest.conCantidadBienes(HOY, 50);
    mision.evaluarProgreso(donante, evento);

    assertTrue(mision.isCompletada());
  }

  @Test
  void habilDonador_conUnBienMenosDelUmbral_noDeberiaCompletarse() {
    MisionHabilDonador mision = MisionMotherTest.habilDonador(CategoriaDonante.SOSTENEDOR, 50);

    EventoDonacion evento = EventoDonacionMotherTest.conCantidadBienes(HOY, 49);
    mision.evaluarProgreso(donante, evento);

    assertFalse(mision.isCompletada());
  }

  @Test
  void habilDonador_noDeberiaCompletarseConDonacionesPequenias() {
    MisionHabilDonador mision = MisionMotherTest.habilDonador(CategoriaDonante.SOSTENEDOR, 50);

    for (int i = 0; i < 10; i++) {
      EventoDonacion evento = EventoDonacionMotherTest.conCantidadBienes(HOY, 5);
      mision.evaluarProgreso(donante, evento);
    }

    assertFalse(mision.isCompletada());
  }

  @Test
  void donacionesExitosas_soloDeberiaContarExitosas() {
    MisionDonacionesExitosas mision = MisionMotherTest.exitosas(CategoriaDonante.TRANSFORMADOR, 3);

    mision.evaluarProgresoExitoso(donante);
    mision.evaluarProgresoExitoso(donante);
    mision.evaluarProgresoExitoso(donante);

    assertTrue(mision.isCompletada());
    assertEquals(3, mision.getProgresoActual());
  }

  @Test
  void mision_deberiaCalcularPorcentajeYDistanciaCorrectamente() {
    MisionDonacionesExitosas mision = MisionMotherTest.exitosas(CategoriaDonante.COLABORADOR, 4);

    mision.evaluarProgresoExitoso(donante);
    mision.evaluarProgresoExitoso(donante);

    assertEquals(50, mision.getPorcentajeProgreso());
    assertEquals(2, mision.getDistanciaAlObjetivo());
  }

  @Test
  void racha_deberiaResetearseAlVerificarVigenciaSiPasoMasDeUnMesSinDonar() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(3);

    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 2, 15));

    racha.verificarVigencia(YearMonth.of(2026, Month.APRIL));

    assertFalse(racha.isCompletada());
    assertEquals(0, racha.getProgresoActual());
  }

  @Test
  void racha_noDeberiaResetearseAlVerificarVigenciaEnElMesSiguienteAlUltimoDonado() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(3);

    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 3, 15));

    racha.verificarVigencia(YearMonth.of(2026, Month.APRIL));

    assertFalse(racha.isCompletada());
    assertEquals(1, racha.getProgresoActual());
  }

  @Test
  void racha_noDeberiaModificarseAlVerificarVigenciaSiYaEstaCompletada() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(2);

    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 2, 15));
    assertTrue(racha.isCompletada());

    racha.verificarVigencia(YearMonth.of(2026, Month.JUNE));

    assertTrue(racha.isCompletada());
    assertEquals(2, racha.getProgresoActual());
  }

  @Test
  void mision_deberiaOtorgarInsigniaAlCompletarse() {
    MisionRacha racha =
        MisionMotherTest.rachaConInsignia(CategoriaDonante.COLABORADOR, 2, "Perseverante");

    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(2026, 2, 15));

    assertTrue(racha.isCompletada());
    assertNotNull(racha.getFechaCompletada());
    assertEquals(1, donante.getInsignias().size());
    assertEquals("Perseverante", donante.getInsignias().getFirst().nombre());
  }

  @Test
  void constructor_deberiaLanzarExcepcionConNombreNuloOVacio() {
    ValidationException ex1 =
        assertThrows(
            ValidationException.class,
            () ->
                new Mision(null, "desc", CategoriaDonante.COLABORADOR, 3) {
                  @Override
                  protected Integer calcularNuevoProgreso(
                      DonanteIncentivos donante, EventoDonacion evento) {
                    return 0;
                  }
                });
    assertEquals(ErrorCatalog.MISION_NOMBRE_INVALIDO, ex1.getError());

    ValidationException ex2 =
        assertThrows(
            ValidationException.class,
            () ->
                new Mision("   ", "desc", CategoriaDonante.COLABORADOR, 3) {
                  @Override
                  protected Integer calcularNuevoProgreso(
                      DonanteIncentivos donante, EventoDonacion evento) {
                    return 0;
                  }
                });
    assertEquals(ErrorCatalog.MISION_NOMBRE_INVALIDO, ex2.getError());
  }

  @Test
  void constructor_deberiaLanzarExcepcionConObjetivoInvalido() {
    ValidationException ex1 =
        assertThrows(
            ValidationException.class, () -> new MisionRacha(CategoriaDonante.COLABORADOR, 0));
    assertEquals(ErrorCatalog.MISION_OBJETIVO_INVALIDO, ex1.getError());

    ValidationException ex2 =
        assertThrows(
            ValidationException.class, () -> new MisionRacha(CategoriaDonante.COLABORADOR, -2));
    assertEquals(ErrorCatalog.MISION_OBJETIVO_INVALIDO, ex2.getError());

    ValidationException ex3 =
        assertThrows(
            ValidationException.class, () -> new MisionRacha(CategoriaDonante.COLABORADOR, null));
    assertEquals(ErrorCatalog.MISION_OBJETIVO_INVALIDO, ex3.getError());
  }

  @Test
  void constructor_deberiaLanzarExcepcionConCategoriaNula() {
    ValidationException ex =
        assertThrows(ValidationException.class, () -> new MisionRacha(null, 3));
    assertEquals(ErrorCatalog.MISION_SIN_CATEGORIA, ex.getError());
  }

  @Test
  void setInsignia_deberiaLanzarExcepcionConInsigniaNula() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(3);
    ValidationException ex = assertThrows(ValidationException.class, () -> racha.setInsignia(null));
    assertEquals(ErrorCatalog.INSIGNIA_NULA, ex.getError());
  }

  @Test
  void misionCompletitud_conCategoriasConEspaciosYMayusculas_debeNormalizarSinDuplicar() {
    MisionCompletitud mision = MisionMotherTest.completitud(CategoriaDonante.COLABORADOR, 2);

    mision.evaluarProgreso(
        donante,
        EventoDonacionMotherTest.conCategorias(
            HOY, List.of("  Alimentos  ", "alimentos", "ALIMENTOS")));

    assertEquals(1, mision.getProgresoActual());
    assertFalse(mision.isCompletada());

    mision.evaluarProgreso(
        donante, EventoDonacionMotherTest.conCategorias(HOY, List.of("  Ropa  ")));

    assertEquals(2, mision.getProgresoActual());
    assertTrue(mision.isCompletada());
  }

  @Test
  void misionCompletar_debePropagarFechaDeDonacionAInsigniaGanada() {
    MisionRacha racha =
        MisionMotherTest.rachaConInsignia(CategoriaDonante.COLABORADOR, 1, "Racha Veloz");
    LocalDate fechaDonacion = LocalDate.of(2026, 3, 20);

    racha.evaluarProgreso(donante, EventoDonacionMotherTest.enFecha(fechaDonacion));

    assertTrue(racha.isCompletada());
    assertEquals(fechaDonacion, racha.getFechaCompletada());
    assertEquals(1, donante.getInsignias().size());
    assertEquals(fechaDonacion, donante.getInsignias().getFirst().fechaObtenida());
  }
}
