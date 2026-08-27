package grupo5.incentivos.models.entities.donante;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.incentivos.fixtures.DonanteIncentivosMotherTest;
import grupo5.incentivos.fixtures.EventoDonacionMotherTest;
import grupo5.incentivos.fixtures.MisionMotherTest;
import grupo5.incentivos.models.entities.donante.eventos.EventoDonanteIncentivos;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonanteIncentivosTest {

  private DonanteIncentivos donante;
  private static final UUID ID_DONANTE = UUID.randomUUID();
  private static final UUID ID_PERSONA = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    donante = DonanteIncentivosMotherTest.colaboradorSinMisiones(ID_DONANTE, ID_PERSONA, "Test");
  }

  @Test
  void verificarRachas_deberiaResetearProgresoSiRachaVencida() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(3);
    DonanteIncentivos d = DonanteIncentivosMotherTest.conMisiones(ID_DONANTE, List.of(racha));

    d.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 1, 15));
    d.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 2, 15));
    assertEquals(2, racha.getProgresoActual());

    // Corre el job en abril (se salteó marzo)
    d.verificarRachas(YearMonth.of(2026, Month.APRIL));

    assertFalse(racha.isCompletada());
    assertEquals(0, racha.getProgresoActual());
  }

  @Test
  void verificarRachas_noDeberiaAfectarMisionVigente() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(3);
    DonanteIncentivos d = DonanteIncentivosMotherTest.conMisiones(ID_DONANTE, List.of(racha));

    d.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 3, 15));
    assertEquals(1, racha.getProgresoActual());

    // Corre el job en abril (mes consecutivo válido)
    d.verificarRachas(YearMonth.of(2026, Month.APRIL));

    assertFalse(racha.isCompletada());
    assertEquals(1, racha.getProgresoActual());
  }

  @Test
  void verificarRachas_noDeberiaModificarMisionYaCompletada() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(2);
    DonanteIncentivos d = DonanteIncentivosMotherTest.conMisiones(ID_DONANTE, List.of(racha));

    d.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 1, 15));
    d.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 2, 15));
    assertTrue(racha.isCompletada());

    d.verificarRachas(YearMonth.of(2026, Month.JUNE));

    assertTrue(racha.isCompletada());
    assertEquals(2, racha.getProgresoActual());
  }

  @Test
  void misionesCompletadas_deberiaContabilizarSoloMisionesCompletadas() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(1);
    MisionDonacionesExitosas exitosas = MisionMotherTest.exitosas(CategoriaDonante.COLABORADOR, 5);
    DonanteIncentivos d =
        DonanteIncentivosMotherTest.conMisiones(ID_DONANTE, List.of(racha, exitosas));

    assertEquals(0, d.misionesCompletadas());

    d.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 5, 15));

    assertTrue(racha.isCompletada());
    assertFalse(exitosas.isCompletada());
    assertEquals(1, d.misionesCompletadas());
  }

  @Test
  void tuvoActividadEnMes_deberiaIndicarSiHuboDonacionesEnElPeriodo() {
    DonanteIncentivos d = DonanteIncentivosMotherTest.colaboradorSinMisiones(ID_DONANTE);

    assertFalse(d.tuvoActividadEnMes(YearMonth.of(2026, Month.MAY)));

    d.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 5, 15));

    assertTrue(d.tuvoActividadEnMes(YearMonth.of(2026, Month.MAY)));
    assertFalse(d.tuvoActividadEnMes(YearMonth.of(2026, Month.JUNE)));
  }

  @Test
  void otorgarInsignia_y_configurarVisibilidad_deberianFuncionarCorrectamente() {
    DonanteIncentivos d = DonanteIncentivosMotherTest.colaboradorSinMisiones(ID_DONANTE);
    Insignia insignia = new Insignia("Compromiso", "Descripción", "http://img.png");

    d.otorgarInsignia(insignia);

    assertEquals(1, d.getInsignias().size());
    assertTrue(d.getInsignias().getFirst().visible());

    d.configurarVisibilidadInsignia("Compromiso", false);

    assertFalse(d.getInsignias().getFirst().visible());
  }

  @Test
  void otorgarInsignia_cuandoInsigniaYaFueOtorgada_noDeberiaDuplicar() {
    DonanteIncentivos d = DonanteIncentivosMotherTest.colaboradorSinMisiones(ID_DONANTE);
    Insignia insignia = new Insignia("Compromiso", "Descripción", "http://img.png");

    d.otorgarInsignia(insignia);
    d.otorgarInsignia(insignia);

    assertEquals(1, d.getInsignias().size());
  }

  @Test
  void getDomainEvents_debeRetornarCopiaInmutableEInmuneAMutacionesPosteriores() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(1);
    DonanteIncentivos d = DonanteIncentivosMotherTest.conMisiones(ID_DONANTE, List.of(racha));

    d.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 5, 15));

    List<EventoDonanteIncentivos> snapshot = d.getDomainEvents();
    assertFalse(snapshot.isEmpty(), "El agregado debe haber registrado eventos de dominio");
    int eventosIniciales = snapshot.size();

    d.clearDomainEvents();

    assertTrue(
        d.getDomainEvents().isEmpty(), "La lista interna del agregado debe haberse limpiado");
    assertEquals(
        eventosIniciales,
        snapshot.size(),
        "El snapshot tomado previamente no debe mutar tras clearDomainEvents()");
    assertThrows(
        UnsupportedOperationException.class,
        () -> snapshot.add(null),
        "El snapshot debe ser una lista inmutable");
  }

  @Test
  void constructor_deberiaLanzarExcepcionConIdDonanteNulo() {
    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> new DonanteIncentivos(null, ID_PERSONA, "Test", List.of()));
    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_ID_NULO, ex.getError());
  }

  @Test
  void constructor_deberiaLanzarExcepcionConIdPersonaNulo() {
    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> new DonanteIncentivos(ID_DONANTE, null, "Test", List.of()));
    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_ID_NULO, ex.getError());
  }

  @Test
  void otorgarInsignia_conFechaEspecifica_debeAsignarFechaCorrecta() {
    DonanteIncentivos d = DonanteIncentivosMotherTest.colaboradorSinMisiones(ID_DONANTE);
    Insignia insignia = new Insignia("Racha Pasada", "3 meses", "http://img.png");
    java.time.LocalDate fecha = java.time.LocalDate.of(2026, 5, 10);

    d.otorgarInsignia(insignia, fecha);

    assertEquals(1, d.getInsignias().size());
    assertEquals(fecha, d.getInsignias().getFirst().fechaObtenida());
  }

  @Test
  void otorgarInsignia_conFechaNula_debeAsignarFechaActual() {
    DonanteIncentivos d = DonanteIncentivosMotherTest.colaboradorSinMisiones(ID_DONANTE);
    Insignia insignia = new Insignia("Compromiso", "Descripción", "http://img.png");

    d.otorgarInsignia(insignia, null);

    assertEquals(1, d.getInsignias().size());
    assertNotNull(d.getInsignias().getFirst().fechaObtenida());
  }

  @Test
  void configurarVisibilidadInsignia_conNombreNuloOVacio_debeLanzarExcepcion() {
    DonanteIncentivos d = DonanteIncentivosMotherTest.colaboradorSinMisiones(ID_DONANTE);

    ValidationException ex1 =
        assertThrows(ValidationException.class, () -> d.configurarVisibilidadInsignia(null, false));
    assertEquals(ErrorCatalog.INSIGNIA_SIN_NOMBRE, ex1.getError());

    ValidationException ex2 =
        assertThrows(
            ValidationException.class, () -> d.configurarVisibilidadInsignia("   ", false));
    assertEquals(ErrorCatalog.INSIGNIA_SIN_NOMBRE, ex2.getError());
  }

  @Test
  void donanteEnCategoriaMaxima_debeRegistrarDonacionesSinErrores() {
    DonanteIncentivos d = DonanteIncentivosMotherTest.conMisiones(ID_DONANTE, List.of());
    // Ascendemos artificialmente al donante a TRANSFORMADOR
    while (d.getCategoria() != CategoriaDonante.TRANSFORMADOR) {
      d.ascender();
    }
    assertEquals(CategoriaDonante.TRANSFORMADOR, d.getCategoria());
    assertNull(d.getMisionActiva());

    // Se registra una nueva donación
    d.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 6, 1));
    d.registrarDonacionExitosa(UUID.randomUUID());

    assertEquals(1, d.getMetricas().getTotalDonacionesHistoricas());
    assertEquals(1, d.getMetricas().getTotalDonacionesExitosas());
  }
}
