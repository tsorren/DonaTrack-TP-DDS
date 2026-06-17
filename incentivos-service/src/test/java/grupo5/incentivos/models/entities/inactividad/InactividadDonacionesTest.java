package grupo5.incentivos.models.entities.inactividad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InactividadDonacionesTest {

  private DonanteIncentivos donanteConUltimaDonacion(LocalDate fecha) {
    DonanteIncentivos donante = new DonanteIncentivos(1L, "Test");
    EventoDonacion evento =
        EventoDonacion.builder()
            .donacionId(1L)
            .fecha(fecha)
            .cantidadBienes(1)
            .categorias(List.of("arroz"))
            .build();
    donante.getMetricas().registrarDonacion(evento);
    return donante;
  }

  @Test
  void detectarInactivos_deberiaDetectarDonanteConUltimaDonacionMuyAntigua() {
    InactividadDonaciones criterio = new InactividadDonaciones(30);
    DonanteIncentivos inactivo = donanteConUltimaDonacion(LocalDate.now().minusDays(60));

    List<DonanteIncentivos> resultado = criterio.detectarInactivos(List.of(inactivo));

    assertEquals(1, resultado.size());
  }

  @Test
  void detectarInactivos_noDeberiaDetectarDonanteQueDonoReciente() {
    InactividadDonaciones criterio = new InactividadDonaciones(30);
    DonanteIncentivos activo = donanteConUltimaDonacion(LocalDate.now().minusDays(5));

    List<DonanteIncentivos> resultado = criterio.detectarInactivos(List.of(activo));

    assertTrue(resultado.isEmpty());
  }

  @Test
  void detectarInactivos_deberiaConsiderarInactivoAlDonanteQueNuncaDono() {
    InactividadDonaciones criterio = new InactividadDonaciones(30);
    DonanteIncentivos sinDonaciones = new DonanteIncentivos(2L, "Nuevo");

    List<DonanteIncentivos> resultado = criterio.detectarInactivos(List.of(sinDonaciones));

    assertEquals(1, resultado.size());
  }

  @Test
  void detectarInactivos_deberiaFiltrarCorrectamenteEntreMixDeActualesEInactivos() {
    InactividadDonaciones criterio = new InactividadDonaciones(30);

    DonanteIncentivos activo = donanteConUltimaDonacion(LocalDate.now().minusDays(10));
    activo.setDonanteId(1L);

    DonanteIncentivos inactivo = donanteConUltimaDonacion(LocalDate.now().minusDays(45));
    inactivo.setDonanteId(2L);

    DonanteIncentivos sinDonaciones = new DonanteIncentivos(3L, "Nuevo");

    List<DonanteIncentivos> resultado =
        criterio.detectarInactivos(List.of(activo, inactivo, sinDonaciones));

    assertEquals(2, resultado.size());
    assertFalse(resultado.stream().anyMatch(d -> d.getDonanteId().equals(1L)));
  }

  @Test
  void detectarInactivos_deberiaRetornarListaVaciaSiNoHayDonantes() {
    InactividadDonaciones criterio = new InactividadDonaciones(30);

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
