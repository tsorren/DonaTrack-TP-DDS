package grupo5.incentivos.models.entities.donante;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.incentivos.models.entities.donante.eventos.EventoDonanteIncentivos;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import java.time.LocalDate;
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
    donante = new DonanteIncentivos(ID_DONANTE, ID_PERSONA, "Test");
  }

  private EventoDonacion eventoEn(int anio, int mes) {
    return EventoDonacion.builder()
        .donacionId(UUID.randomUUID())
        .fecha(LocalDate.of(anio, mes, 15))
        .cantidadBienes(5)
        .categorias(List.of("alimentos"))
        .build();
  }

  @Test
  void verificarRachas_deberiaResetearProgresoSiRachaVencida() {
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 3);
    DonanteIncentivos d = new DonanteIncentivos(ID_DONANTE, ID_PERSONA, "Test", List.of(racha));

    d.registrarDonacion(eventoEn(2026, 1));
    d.registrarDonacion(eventoEn(2026, 2));
    assertEquals(2, racha.getProgresoActual());

    // Corre el job en abril (se salteó marzo)
    d.verificarRachas(YearMonth.of(2026, Month.APRIL));

    assertFalse(racha.isCompletada());
    assertEquals(0, racha.getProgresoActual());
  }

  @Test
  void verificarRachas_noDeberiaAfectarMisionVigente() {
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 3);
    DonanteIncentivos d = new DonanteIncentivos(ID_DONANTE, ID_PERSONA, "Test", List.of(racha));

    d.registrarDonacion(eventoEn(2026, 3));
    assertEquals(1, racha.getProgresoActual());

    // Corre el job en abril (mes consecutivo válido)
    d.verificarRachas(YearMonth.of(2026, Month.APRIL));

    assertFalse(racha.isCompletada());
    assertEquals(1, racha.getProgresoActual());
  }

  @Test
  void verificarRachas_noDeberiaModificarMisionYaCompletada() {
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 2);
    DonanteIncentivos d = new DonanteIncentivos(ID_DONANTE, ID_PERSONA, "Test", List.of(racha));

    d.registrarDonacion(eventoEn(2026, 1));
    d.registrarDonacion(eventoEn(2026, 2));
    assertTrue(racha.isCompletada());

    d.verificarRachas(YearMonth.of(2026, Month.JUNE));

    assertTrue(racha.isCompletada());
    assertEquals(2, racha.getProgresoActual());
  }

  @Test
  void misionesCompletadas_deberiaContabilizarSoloMisionesCompletadas() {
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
    MisionDonacionesExitosas exitosas =
        new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 5);
    DonanteIncentivos d =
        new DonanteIncentivos(ID_DONANTE, ID_PERSONA, "Test", List.of(racha, exitosas));

    assertEquals(0, d.misionesCompletadas());

    d.registrarDonacion(eventoEn(2026, 5));

    assertTrue(racha.isCompletada());
    assertFalse(exitosas.isCompletada());
    assertEquals(1, d.misionesCompletadas());
  }

  @Test
  void tuvoActividadEnMes_deberiaIndicarSiHuboDonacionesEnElPeriodo() {
    DonanteIncentivos d = new DonanteIncentivos(ID_DONANTE, ID_PERSONA, "Test", List.of());

    assertFalse(d.tuvoActividadEnMes(YearMonth.of(2026, Month.MAY)));

    d.registrarDonacion(eventoEn(2026, 5));

    assertTrue(d.tuvoActividadEnMes(YearMonth.of(2026, Month.MAY)));
    assertFalse(d.tuvoActividadEnMes(YearMonth.of(2026, Month.JUNE)));
  }

  @Test
  void otorgarInsignia_y_configurarVisibilidad_deberianFuncionarCorrectamente() {
    DonanteIncentivos d = new DonanteIncentivos(ID_DONANTE, ID_PERSONA, "Test", List.of());
    Insignia insignia = new Insignia("Compromiso", "Descripción", "http://img.png");

    d.otorgarInsignia(insignia);

    assertEquals(1, d.getInsignias().size());
    assertTrue(d.getInsignias().getFirst().visible());

    d.configurarVisibilidadInsignia("Compromiso", false);

    assertFalse(d.getInsignias().getFirst().visible());
  }

  @Test
  void getDomainEvents_debeRetornarCopiaInmutableEInmuneAMutacionesPosteriores() {
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
    DonanteIncentivos d = new DonanteIncentivos(ID_DONANTE, ID_PERSONA, "Test", List.of(racha));

    // Evento que completa la misión y registra eventos
    d.registrarDonacion(eventoEn(2026, 5));

    List<EventoDonanteIncentivos> snapshot = d.getDomainEvents();
    assertFalse(snapshot.isEmpty(), "El agregado debe haber registrado eventos de dominio");
    int eventosIniciales = snapshot.size();

    // Mutación posterior sobre el agregado
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
}
