package grupo5.incentivos.models.entities.donante.misiones;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.fixtures.EventoDonacionMother;
import grupo5.incentivos.fixtures.MisionMother;
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
    donante = DonanteIncentivosMother.colaboradorSinMisiones();
  }

  @Test
  void racha_deberiaCompletarseConMesesConsecutivos() {
    MisionRacha racha = MisionMother.rachaColaborador(3);

    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 2, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 3, 15));

    assertTrue(racha.isCompletada());
    assertEquals(3, racha.getProgresoActual());
  }

  @Test
  void racha_deberiaResetearseAlSaltarUnMes() {
    MisionRacha racha = MisionMother.rachaColaborador(3);

    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 2, 15));

    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 4, 15));

    assertFalse(racha.isCompletada());
    assertEquals(1, racha.getProgresoActual());
  }

  @Test
  void racha_noDeberiaContarDosDonacionesDelMismoMes() {
    MisionRacha racha = MisionMother.rachaColaborador(3);

    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 1, 20)); // mismo mes
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 2, 15));

    assertFalse(racha.isCompletada());
    assertEquals(2, racha.getProgresoActual());
  }

  @Test
  void completitud_deberiaContarSubcategoriasUnicas() {
    MisionCompletitud mision = MisionMother.completitud(CategoriaDonante.COLABORADOR, 3);

    mision.evaluarProgreso(donante, EventoDonacionMother.conCategorias(HOY, List.of("arroz")));
    mision.evaluarProgreso(donante, EventoDonacionMother.conCategorias(HOY, List.of("ropa")));
    mision.evaluarProgreso(donante, EventoDonacionMother.conCategorias(HOY, List.of("arroz")));
    mision.evaluarProgreso(donante, EventoDonacionMother.conCategorias(HOY, List.of("sillas")));

    assertTrue(mision.isCompletada());
    assertEquals(3, mision.getProgresoActual());
  }

  @Test
  void completitud_conMultiplesCategoriasEnMismaDonacion_deberiaContabilizarTodasLasUnicas() {
    MisionCompletitud mision = MisionMother.completitud(CategoriaDonante.COLABORADOR, 3);

    mision.evaluarProgreso(
        donante, EventoDonacionMother.conCategorias(HOY, List.of("arroz", "leche", "fideos")));

    assertTrue(mision.isCompletada());
    assertEquals(3, mision.getProgresoActual());
  }

  @Test
  void habilDonador_deberiaCompletarseConUnaGranDonacion() {
    MisionHabilDonador mision = MisionMother.habilDonador(CategoriaDonante.SOSTENEDOR, 50);

    EventoDonacion evento = EventoDonacionMother.conCantidadBienes(HOY, 55);
    mision.evaluarProgreso(donante, evento);

    assertTrue(mision.isCompletada());
  }

  @Test
  void habilDonador_conExactamenteElUmbral_deberiaCompletarse() {
    MisionHabilDonador mision = MisionMother.habilDonador(CategoriaDonante.SOSTENEDOR, 50);

    EventoDonacion evento = EventoDonacionMother.conCantidadBienes(HOY, 50);
    mision.evaluarProgreso(donante, evento);

    assertTrue(mision.isCompletada());
  }

  @Test
  void habilDonador_conUnBienMenosDelUmbral_noDeberiaCompletarse() {
    MisionHabilDonador mision = MisionMother.habilDonador(CategoriaDonante.SOSTENEDOR, 50);

    EventoDonacion evento = EventoDonacionMother.conCantidadBienes(HOY, 49);
    mision.evaluarProgreso(donante, evento);

    assertFalse(mision.isCompletada());
  }

  @Test
  void habilDonador_noDeberiaCompletarseConDonacionesPequenias() {
    MisionHabilDonador mision = MisionMother.habilDonador(CategoriaDonante.SOSTENEDOR, 50);

    for (int i = 0; i < 10; i++) {
      EventoDonacion evento = EventoDonacionMother.conCantidadBienes(HOY, 5);
      mision.evaluarProgreso(donante, evento);
    }

    assertFalse(mision.isCompletada());
  }

  @Test
  void donacionesExitosas_soloDeberiaContarExitosas() {
    MisionDonacionesExitosas mision = MisionMother.exitosas(CategoriaDonante.TRANSFORMADOR, 3);

    mision.evaluarProgresoExitoso(donante);
    mision.evaluarProgresoExitoso(donante);
    mision.evaluarProgresoExitoso(donante);

    assertTrue(mision.isCompletada());
    assertEquals(3, mision.getProgresoActual());
  }

  @Test
  void mision_deberiaCalcularPorcentajeYDistanciaCorrectamente() {
    MisionDonacionesExitosas mision = MisionMother.exitosas(CategoriaDonante.COLABORADOR, 4);

    mision.evaluarProgresoExitoso(donante);
    mision.evaluarProgresoExitoso(donante);

    assertEquals(50, mision.getPorcentajeProgreso());
    assertEquals(2, mision.getDistanciaAlObjetivo());
  }

  @Test
  void racha_deberiaResetearseAlVerificarVigenciaSiPasoMasDeUnMesSinDonar() {
    MisionRacha racha = MisionMother.rachaColaborador(3);

    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 2, 15));

    racha.verificarVigencia(YearMonth.of(2026, Month.APRIL));

    assertFalse(racha.isCompletada());
    assertEquals(0, racha.getProgresoActual());
  }

  @Test
  void racha_noDeberiaResetearseAlVerificarVigenciaEnElMesSiguienteAlUltimoDonado() {
    MisionRacha racha = MisionMother.rachaColaborador(3);

    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 3, 15));

    racha.verificarVigencia(YearMonth.of(2026, Month.APRIL));

    assertFalse(racha.isCompletada());
    assertEquals(1, racha.getProgresoActual());
  }

  @Test
  void racha_noDeberiaModificarseAlVerificarVigenciaSiYaEstaCompletada() {
    MisionRacha racha = MisionMother.rachaColaborador(2);

    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 2, 15));
    assertTrue(racha.isCompletada());

    racha.verificarVigencia(YearMonth.of(2026, Month.JUNE));

    assertTrue(racha.isCompletada());
    assertEquals(2, racha.getProgresoActual());
  }

  @Test
  void mision_deberiaOtorgarInsigniaAlCompletarse() {
    MisionRacha racha =
        MisionMother.rachaConInsignia(CategoriaDonante.COLABORADOR, 2, "Perseverante");

    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 2, 15));

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
    MisionRacha racha = MisionMother.rachaColaborador(3);
    ValidationException ex = assertThrows(ValidationException.class, () -> racha.setInsignia(null));
    assertEquals(ErrorCatalog.INSIGNIA_NULA, ex.getError());
  }

  @Test
  void misionCompletitud_conCategoriasConEspaciosYMayusculas_debeNormalizarSinDuplicar() {
    MisionCompletitud mision = MisionMother.completitud(CategoriaDonante.COLABORADOR, 2);

    mision.evaluarProgreso(
        donante,
        EventoDonacionMother.conCategorias(
            HOY, List.of("  Alimentos  ", "alimentos", "ALIMENTOS")));

    assertEquals(1, mision.getProgresoActual());
    assertFalse(mision.isCompletada());

    mision.evaluarProgreso(donante, EventoDonacionMother.conCategorias(HOY, List.of("  Ropa  ")));

    assertEquals(2, mision.getProgresoActual());
    assertTrue(mision.isCompletada());
  }

  @Test
  void misionCompletar_debePropagarFechaDeDonacionAInsigniaGanada() {
    MisionRacha racha =
        MisionMother.rachaConInsignia(CategoriaDonante.COLABORADOR, 1, "Racha Veloz");
    LocalDate fechaDonacion = LocalDate.of(2026, Month.MARCH, 20);

    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(fechaDonacion));

    assertTrue(racha.isCompletada());
    assertEquals(fechaDonacion, racha.getFechaCompletada());
    assertEquals(1, donante.getInsignias().size());
    assertEquals(fechaDonacion, donante.getInsignias().getFirst().fechaObtenida());
  }

  @Test
  void racha_conEventoDiferidoDeMesAnterior_noDeberiaResetearProgresoAvanzado() {
    MisionRacha racha = MisionMother.rachaColaborador(5);

    // Donaciones en Enero, Febrero, Marzo -> Progreso 3
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 1, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 2, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 3, 15));
    assertEquals(3, racha.getProgresoActual());

    // Evento diferido recibido de Febrero (mes anterior al último registrado Marzo)
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 2, 28));

    // El progreso debe mantenerse intacto en 3 y no resetearse a 1
    assertEquals(3, racha.getProgresoActual());
    assertFalse(racha.isCompletada());

    // Donación en Abril -> Continúa la racha a 4
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 4, 15));
    assertEquals(4, racha.getProgresoActual());
  }

  @Test
  void racha_conDonacionDemoradaDelMesAnteriorAlPrimeroDeLaRacha_deberiaExtenderlaHaciaAtras() {
    MisionRacha racha = MisionMother.rachaColaborador(5);

    // Febrero, Marzo, Abril -> racha de 3 (primer mes: Febrero)
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 2, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 3, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 4, 15));
    assertEquals(3, racha.getProgresoActual());

    // Llega demorada la donación de Enero (mes anterior al primero de la racha)
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 1, 20));

    assertEquals(4, racha.getProgresoActual());
    assertFalse(racha.isCompletada());

    // La racha sigue extendiéndose hacia adelante con normalidad (Mayo)
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 5, 10));

    assertEquals(5, racha.getProgresoActual());
    assertTrue(racha.isCompletada());
  }

  @Test
  void racha_conDonacionViejaQueNoConectaConLaRachaActual_deberiaIgnorarseSinRomperla() {
    MisionRacha racha = MisionMother.rachaColaborador(5);

    // Febrero, Marzo, Abril -> racha de 3 (primer mes: Febrero)
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 2, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 3, 15));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 4, 15));
    assertEquals(3, racha.getProgresoActual());

    // Llega una donación de Diciembre 2025: no conecta (no es el mes inmediato anterior)
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2025, 12, 5));

    assertEquals(3, racha.getProgresoActual());
    assertFalse(racha.isCompletada());

    // La racha sigue vigente y extendiéndose con normalidad
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 5, 10));
    assertEquals(4, racha.getProgresoActual());
  }

  @Test
  void mision_yaCompletada_alEvaluarProgresoNuevamente_noDeberiaAlterarSuEstado() {
    MisionRacha racha = MisionMother.rachaColaborador(1);
    LocalDate fechaCompletada = LocalDate.of(2026, Month.JANUARY, 15);

    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(fechaCompletada));
    assertTrue(racha.isCompletada());
    assertEquals(1, racha.getProgresoActual());

    // Un evento posterior, que de procesarse normalmente reiniciaría la racha, no debe afectarla
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 6, 15));

    assertTrue(racha.isCompletada());
    assertEquals(1, racha.getProgresoActual());
    assertEquals(fechaCompletada, racha.getFechaCompletada());
    assertEquals(YearMonth.of(2026, Month.JANUARY), racha.getUltimoMesDonado());
  }

  @Test
  void fueCompletadaEnMes_conMesIgualPeroAnioDistinto_deberiaSerFalso() {
    MisionRacha racha = MisionMother.rachaColaborador(1);
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 5, 10));
    assertTrue(racha.isCompletada());

    assertTrue(racha.fueCompletadaEnMes(2026, 5));
    assertFalse(racha.fueCompletadaEnMes(2027, 5));
  }

  @Test
  void completitud_conListaDeCategoriasVacia_noDeberiaSumarProgreso() {
    MisionCompletitud mision = MisionMother.completitud(CategoriaDonante.COLABORADOR, 2);

    mision.evaluarProgreso(donante, EventoDonacionMother.conCategorias(HOY, List.of()));

    assertEquals(0, mision.getProgresoActual());
    assertFalse(mision.isCompletada());
  }

  @Test
  void completitud_conCategoriasVaciasOEnBlancoEnLaLista_deberiaIgnorarlasSinContarlas() {
    MisionCompletitud mision = MisionMother.completitud(CategoriaDonante.COLABORADOR, 2);

    mision.evaluarProgreso(
        donante, EventoDonacionMother.conCategorias(HOY, List.of("", "   ", "arroz")));

    assertEquals(1, mision.getProgresoActual());
    assertFalse(mision.isCompletada());
  }

  @Test
  void habilDonador_conCantidadDeBienesNulaEnElEvento_deberiaTratarseComoCero() {
    MisionHabilDonador mision = MisionMother.habilDonador(CategoriaDonante.SOSTENEDOR, 5);
    EventoDonacion evento =
        EventoDonacion.builder().fecha(HOY).cantidadBienes(null).categorias(List.of()).build();

    mision.evaluarProgreso(donante, evento);

    assertEquals(0, mision.getProgresoActual());
    assertFalse(mision.isCompletada());
  }
}
