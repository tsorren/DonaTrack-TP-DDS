package grupo5.incentivos.models.entities.inactividad;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InactividadDonacionesTest {

  private static final LocalDate HOY = LocalDate.of(2026, Month.JUNE, 17);

  private InactividadDonaciones crearCriterio(int diasSinDonar) {
    Clock fixedClock =
        Clock.fixed(HOY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    return new InactividadDonaciones(diasSinDonar, fixedClock);
  }

  private DonanteIncentivos donanteConUltimaDonacion(UUID id, LocalDate fecha) {
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    EventoDonacion evento =
        EventoDonacion.builder()
            .donacionId(UUID.randomUUID())
            .fecha(fecha)
            .cantidadBienes(1)
            .categorias(List.of("arroz"))
            .build();
    donante.getMetricas().registrarDonacion(evento);
    return donante;
  }

  @Test
  void detectarInactivos_deberiaDetectarDonanteConUltimaDonacionMuyAntigua() {
    InactividadDonaciones criterio = crearCriterio(30);
    DonanteIncentivos inactivo = donanteConUltimaDonacion(UUID.randomUUID(), HOY.minusDays(60));

    List<DonanteIncentivos> resultado = criterio.detectarInactivos(List.of(inactivo));

    assertEquals(1, resultado.size());
  }

  @Test
  void detectarInactivos_noDeberiaDetectarDonanteQueDonoReciente() {
    InactividadDonaciones criterio = crearCriterio(30);
    DonanteIncentivos activo = donanteConUltimaDonacion(UUID.randomUUID(), HOY.minusDays(5));

    List<DonanteIncentivos> resultado = criterio.detectarInactivos(List.of(activo));

    assertTrue(resultado.isEmpty());
  }

  @Test
  void detectarInactivos_deberiaConsiderarInactivoAlDonanteQueNuncaDono() {
    InactividadDonaciones criterio = crearCriterio(30);
    UUID id = UUID.randomUUID();
    DonanteIncentivos sinDonaciones = new DonanteIncentivos(id, id, "Nuevo");

    List<DonanteIncentivos> resultado = criterio.detectarInactivos(List.of(sinDonaciones));

    assertEquals(1, resultado.size());
  }

  @Test
  void detectarInactivos_deberiaFiltrarCorrectamenteEntreMixDeActualesEInactivos() {
    InactividadDonaciones criterio = crearCriterio(30);

    UUID idActivo = new UUID(0L, 1L);
    UUID idInactivo = new UUID(0L, 2L);
    UUID idSinDonaciones = new UUID(0L, 3L);

    DonanteIncentivos activo = donanteConUltimaDonacion(idActivo, HOY.minusDays(10));
    DonanteIncentivos inactivo = donanteConUltimaDonacion(idInactivo, HOY.minusDays(45));
    DonanteIncentivos sinDonaciones =
        new DonanteIncentivos(idSinDonaciones, idSinDonaciones, "Nuevo");

    List<DonanteIncentivos> resultado =
        criterio.detectarInactivos(List.of(activo, inactivo, sinDonaciones));

    assertEquals(2, resultado.size());
    assertFalse(resultado.stream().anyMatch(d -> d.getId().equals(idActivo)));
  }

  @Test
  void detectarInactivos_deberiaRetornarListaVaciaSiNoHayDonantes() {
    InactividadDonaciones criterio = crearCriterio(30);

    List<DonanteIncentivos> resultado = criterio.detectarInactivos(List.of());

    assertTrue(resultado.isEmpty());
  }

  @Test
  void constructor_deberiaLanzarExcepcionConDiasCero() {
    assertThrows(IllegalArgumentException.class, () -> new InactividadDonaciones(0));
  }

  @Test
  void constructor_deberiaLanzarExcepcionConDiasNegativos() {
    assertThrows(IllegalArgumentException.class, () -> new InactividadDonaciones(-5));
  }
}
