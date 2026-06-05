package grupo5.incentivos.models.entities.misiones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MisionesTest {

  private DonanteIncentivos donante;

  @BeforeEach
  void setUp() {
    donante = new DonanteIncentivos(1L);
  }

  private EventoDonacion eventoEn(int anio, int mes) {
    return EventoDonacion.builder()
        .donacionId(1L)
        .fecha(LocalDate.of(anio, mes, 15))
        .exitosa(true)
        .cantidadBienes(5)
        .subcategoria("arroz")
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
            .donacionId(1L)
            .fecha(LocalDate.now())
            .exitosa(true)
            .cantidadBienes(1)
            .subcategoria("arroz")
            .build();
    EventoDonacion e2 =
        EventoDonacion.builder()
            .donacionId(2L)
            .fecha(LocalDate.now())
            .exitosa(true)
            .cantidadBienes(1)
            .subcategoria("ropa")
            .build();
    EventoDonacion e3 =
        EventoDonacion.builder()
            .donacionId(3L)
            .fecha(LocalDate.now())
            .exitosa(true)
            .cantidadBienes(1)
            .subcategoria("arroz")
            .build();
    EventoDonacion e4 =
        EventoDonacion.builder()
            .donacionId(4L)
            .fecha(LocalDate.now())
            .exitosa(true)
            .cantidadBienes(1)
            .subcategoria("sillas")
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
            .donacionId(1L)
            .fecha(LocalDate.now())
            .exitosa(true)
            .cantidadBienes(55)
            .subcategoria("fideos")
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
              .donacionId((long) i)
              .fecha(LocalDate.now())
              .exitosa(true)
              .cantidadBienes(5)
              .subcategoria("fideos")
              .build();
      mision.evaluarProgreso(donante, evento);
    }

    assertFalse(mision.isCompletada());
  }

  @Test
  void donacionesExitosas_soloDeberiaContarExitosas() {
    MisionDonacionesExitosas mision =
        new MisionDonacionesExitosas(CategoriaDonante.TRANSFORMADOR, 3);

    EventoDonacion exitosa =
        EventoDonacion.builder()
            .donacionId(1L)
            .fecha(LocalDate.now())
            .exitosa(true)
            .cantidadBienes(1)
            .subcategoria("x")
            .build();
    EventoDonacion fallida =
        EventoDonacion.builder()
            .donacionId(2L)
            .fecha(LocalDate.now())
            .exitosa(false)
            .cantidadBienes(1)
            .subcategoria("x")
            .build();

    mision.evaluarProgreso(donante, exitosa);
    mision.evaluarProgreso(donante, fallida);
    mision.evaluarProgreso(donante, exitosa);
    mision.evaluarProgreso(donante, exitosa);

    assertTrue(mision.isCompletada());
    assertEquals(3, mision.getProgresoActual());
  }

  @Test
  void mision_deberiaCalcularPorcentajeCorrectamente() {
    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 4);

    EventoDonacion exitosa =
        EventoDonacion.builder()
            .donacionId(1L)
            .fecha(LocalDate.now())
            .exitosa(true)
            .cantidadBienes(1)
            .subcategoria("x")
            .build();

    mision.evaluarProgreso(donante, exitosa);
    mision.evaluarProgreso(donante, exitosa);

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
    assertEquals("Perseverante", donante.getInsignias().get(0).getNombre());
  }
}
