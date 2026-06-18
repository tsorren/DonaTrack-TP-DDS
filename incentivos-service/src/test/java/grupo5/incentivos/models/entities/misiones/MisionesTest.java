package grupo5.incentivos.models.entities.misiones;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MisionesTest {

  private static final LocalDate HOY = LocalDate.of(2026, Month.JUNE, 15);

  private DonanteIncentivos donante;

  @BeforeEach
  void setUp() {
    donante = new DonanteIncentivos(new UUID(0L, 1L), new UUID(0L, 1L), "Test");
  }

  private EventoDonacion eventoEn(int anio, int mes) {
    return EventoDonacion.builder()
        .donacionId(new UUID(0L, 1L))
        .fecha(LocalDate.of(anio, mes, 15))
        .cantidadBienes(5)
        .categorias(List.of("arroz"))
        .build();
  }

  @Test
  void racha_deberiaCompletarseConMesesConsecutivos() {
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 3);

    racha.evaluarProgreso(donante, eventoEn(2026, 1));
    racha.evaluarProgreso(donante, eventoEn(2026, 2));
    racha.evaluarProgreso(donante, eventoEn(2026, 3));

    assertTrue(racha.isCompletada());
    assertEquals(3, racha.getProgresoActual());
  }

  @Test
  void racha_deberiaResetearseAlSaltarUnMes() {
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 3);

    racha.evaluarProgreso(donante, eventoEn(2026, 1));
    racha.evaluarProgreso(donante, eventoEn(2026, 2));

    racha.evaluarProgreso(donante, eventoEn(2026, 4));

    assertFalse(racha.isCompletada());
    assertEquals(1, racha.getProgresoActual());
  }

  @Test
  void racha_noDeberiaContarDosDonacionesDelMismoMes() {
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 3);

    racha.evaluarProgreso(donante, eventoEn(2026, 1));
    racha.evaluarProgreso(donante, eventoEn(2026, 1)); // mismo mes
    racha.evaluarProgreso(donante, eventoEn(2026, 2));

    assertFalse(racha.isCompletada());
    assertEquals(2, racha.getProgresoActual());
  }

  @Test
  void completitud_deberiaContarSubcategoriasUnicas() {
    MisionCompletitud mision = new MisionCompletitud(CategoriaDonante.COLABORADOR, 3);

    EventoDonacion e1 =
        EventoDonacion.builder()
            .donacionId(new UUID(0L, 1L))
            .fecha(HOY)
            .cantidadBienes(1)
            .categorias(List.of("arroz"))
            .build();
    EventoDonacion e2 =
        EventoDonacion.builder()
            .donacionId(new UUID(0L, 2L))
            .fecha(HOY)
            .cantidadBienes(1)
            .categorias(List.of("ropa"))
            .build();
    EventoDonacion e3 =
        EventoDonacion.builder()
            .donacionId(new UUID(0L, 3L))
            .fecha(HOY)
            .cantidadBienes(1)
            .categorias(List.of("arroz"))
            .build();
    EventoDonacion e4 =
        EventoDonacion.builder()
            .donacionId(new UUID(0L, 4L))
            .fecha(HOY)
            .cantidadBienes(1)
            .categorias(List.of("sillas"))
            .build();

    mision.evaluarProgreso(donante, e1);
    mision.evaluarProgreso(donante, e2);
    mision.evaluarProgreso(donante, e3);
    mision.evaluarProgreso(donante, e4);

    assertTrue(mision.isCompletada());
    assertEquals(3, mision.getProgresoActual());
  }

  @Test
  void habilDonador_deberiaCompletarseConUnaGranDonacion() {
    MisionHabilDonador mision = new MisionHabilDonador(CategoriaDonante.SOSTENEDOR, 50);

    EventoDonacion evento =
        EventoDonacion.builder()
            .donacionId(new UUID(0L, 1L))
            .fecha(HOY)
            .cantidadBienes(55)
            .categorias(List.of("fideos"))
            .build();

    mision.evaluarProgreso(donante, evento);

    assertTrue(mision.isCompletada());
  }

  @Test
  void habilDonador_noDeberiaCompletarseConDonacionesPequenias() {
    MisionHabilDonador mision = new MisionHabilDonador(CategoriaDonante.SOSTENEDOR, 50);

    for (int i = 0; i < 10; i++) {
      EventoDonacion evento =
          EventoDonacion.builder()
              .donacionId(new UUID(0L, (long) i))
              .fecha(HOY)
              .cantidadBienes(5)
              .categorias(List.of("fideos"))
              .build();
      mision.evaluarProgreso(donante, evento);
    }

    assertFalse(mision.isCompletada());
  }

  @Test
  void donacionesExitosas_soloDeberiaContarExitosas() {
    MisionDonacionesExitosas mision =
        new MisionDonacionesExitosas(CategoriaDonante.TRANSFORMADOR, 3);

    mision.evaluarProgresoExitoso(donante);
    // donacion no exitosa: no se llama evaluarProgresoExitoso
    mision.evaluarProgresoExitoso(donante);
    mision.evaluarProgresoExitoso(donante);

    assertTrue(mision.isCompletada());
    assertEquals(3, mision.getProgresoActual());
  }

  @Test
  void mision_deberiaCalcularPorcentajeCorrectamente() {
    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 4);

    mision.evaluarProgresoExitoso(donante);
    mision.evaluarProgresoExitoso(donante);

    assertEquals(50, mision.getPorcentajeProgreso());
    assertEquals(2, mision.getDistanciaAlObjetivo());
  }

  @Test
  void mision_deberiaOtorgarInsigniaAlCompletarse() {
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 2);
    grupo5.incentivos.models.entities.insignias.Insignia insignia =
        new grupo5.incentivos.models.entities.insignias.Insignia(
            "Perseverante", "2 meses seguidos", "/img.png");
    racha.setInsignia(insignia);

    racha.evaluarProgreso(donante, eventoEn(2026, 1));
    racha.evaluarProgreso(donante, eventoEn(2026, 2));

    assertTrue(racha.isCompletada());
    assertNotNull(racha.getFechaCompletada());
    assertEquals(1, donante.getInsignias().size());
    assertEquals("Perseverante", donante.getInsignias().getFirst().getNombre());
  }
}
