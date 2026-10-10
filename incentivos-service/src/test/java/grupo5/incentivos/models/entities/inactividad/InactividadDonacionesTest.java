package grupo5.incentivos.models.entities.inactividad;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.fixtures.EventoDonacionMother;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InactividadDonacionesTest {

  private static final LocalDate HOY = LocalDate.now();

  private InactividadDonaciones crearCriterio(int diasSinDonar) {
    Clock fixedClock =
        Clock.fixed(HOY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    return new InactividadDonaciones(diasSinDonar, fixedClock);
  }

  @Test
  void detectarInactivos_deberiaDetectarDonanteConUltimaDonacionMuyAntigua() {
    InactividadDonaciones criterio = crearCriterio(30);
    UUID id = UUID.randomUUID();
    DonanteIncentivos inactivo =
        DonanteIncentivosMother.conDonacion(id, EventoDonacionMother.enFecha(HOY.minusDays(60)));

    List<DonanteInactivo> resultado = criterio.detectarInactivos(List.of(inactivo));

    assertEquals(1, resultado.size());
    assertEquals(inactivo.getId(), resultado.getFirst().idDonante());
    assertEquals(inactivo.getIdPersona(), resultado.getFirst().idPersona());
    assertEquals(60, resultado.getFirst().diasInactivo());
    assertEquals(HOY, resultado.getFirst().fecha());
  }

  @Test
  void detectarInactivos_noDeberiaDetectarDonanteQueDonoReciente() {
    InactividadDonaciones criterio = crearCriterio(30);
    UUID id = UUID.randomUUID();
    DonanteIncentivos activo =
        DonanteIncentivosMother.conDonacion(id, EventoDonacionMother.enFecha(HOY.minusDays(5)));

    List<DonanteInactivo> resultado = criterio.detectarInactivos(List.of(activo));

    assertTrue(resultado.isEmpty());
  }

  @Test
  void detectarInactivos_enElLimiteExactoDeDias_noDeberiaConsiderarseInactivo() {
    InactividadDonaciones criterio = crearCriterio(30);
    UUID id = UUID.randomUUID();
    DonanteIncentivos activoEnLimite =
        DonanteIncentivosMother.conDonacion(id, EventoDonacionMother.enFecha(HOY.minusDays(30)));

    List<DonanteInactivo> resultado = criterio.detectarInactivos(List.of(activoEnLimite));

    assertTrue(resultado.isEmpty());
  }

  @Test
  void detectarInactivos_conUnDiaMasDelLimite_deberiaConsiderarseInactivo() {
    InactividadDonaciones criterio = crearCriterio(30);
    UUID id = UUID.randomUUID();
    DonanteIncentivos inactivo =
        DonanteIncentivosMother.conDonacion(id, EventoDonacionMother.enFecha(HOY.minusDays(31)));

    List<DonanteInactivo> resultado = criterio.detectarInactivos(List.of(inactivo));

    assertEquals(1, resultado.size());
    assertEquals(31, resultado.getFirst().diasInactivo());
  }

  @Test
  void detectarInactivos_noDeberiaDetectarDonanteRecienRegistradoSinDonaciones() {
    InactividadDonaciones criterio = crearCriterio(30);
    UUID id = UUID.randomUUID();
    DonanteIncentivos recienRegistrado = DonanteIncentivosMother.colaboradorRegistradoEn(id, HOY);

    List<DonanteInactivo> resultado = criterio.detectarInactivos(List.of(recienRegistrado));

    assertTrue(
        resultado.isEmpty(),
        "Un donante registrado hoy sin donaciones no debe considerarse inactivo");
  }

  @Test
  void detectarInactivos_deberiaConsiderarInactivoAlDonanteQueNuncaDonoYRegistroAntiguo() {
    InactividadDonaciones criterio = crearCriterio(30);
    UUID id = UUID.randomUUID();
    DonanteIncentivos registroAntiguo =
        DonanteIncentivosMother.colaboradorRegistradoEn(id, HOY.minusDays(45));

    List<DonanteInactivo> resultado = criterio.detectarInactivos(List.of(registroAntiguo));

    assertEquals(1, resultado.size());
    assertEquals(id, resultado.get(0).idDonante());
    assertEquals(45, resultado.get(0).diasInactivo());
  }

  @Test
  void detectarInactivos_deberiaFiltrarCorrectamenteEntreMixDeActualesEInactivos() {
    InactividadDonaciones criterio = crearCriterio(30);

    UUID idActivo = new UUID(0L, 1L);
    UUID idInactivo = new UUID(0L, 2L);
    UUID idSinDonacionesAntiguo = new UUID(0L, 3L);

    DonanteIncentivos activo =
        DonanteIncentivosMother.conDonacion(
            idActivo, EventoDonacionMother.enFecha(HOY.minusDays(10)));
    DonanteIncentivos inactivo =
        DonanteIncentivosMother.conDonacion(
            idInactivo, EventoDonacionMother.enFecha(HOY.minusDays(45)));
    DonanteIncentivos sinDonacionesAntiguo =
        DonanteIncentivosMother.colaboradorRegistradoEn(idSinDonacionesAntiguo, HOY.minusDays(40));

    List<DonanteInactivo> resultado =
        criterio.detectarInactivos(List.of(activo, inactivo, sinDonacionesAntiguo));

    assertEquals(2, resultado.size());
    assertFalse(resultado.stream().anyMatch(d -> d.idDonante().equals(idActivo)));
  }

  @Test
  void detectarInactivos_deberiaRetornarListaVaciaSiNoHayDonantes() {
    InactividadDonaciones criterio = crearCriterio(30);

    List<DonanteInactivo> resultado = criterio.detectarInactivos(List.of());

    assertTrue(resultado.isEmpty());
  }

  @Test
  void constructor_deberiaLanzarExcepcionConDiasCero() {
    ValidationException ex =
        assertThrows(ValidationException.class, () -> new InactividadDonaciones(0));
    assertEquals(ErrorCatalog.INACTIVIDAD_DIAS_INVALIDOS, ex.getError());
  }

  @Test
  void constructor_deberiaLanzarExcepcionConDiasNegativos() {
    ValidationException ex =
        assertThrows(ValidationException.class, () -> new InactividadDonaciones(-5));
    assertEquals(ErrorCatalog.INACTIVIDAD_DIAS_INVALIDOS, ex.getError());
  }

  @Test
  void gestorDeInactivos_conMultiplesCriterios_deberiaDeduplicarPorPersonaReteniendoMaximoDias() {
    GestorDeInactivos gestor = new GestorDeInactivos();
    UUID id = UUID.randomUUID();
    DonanteIncentivos inactivo =
        DonanteIncentivosMother.conDonacion(id, EventoDonacionMother.enFecha(HOY.minusDays(50)));

    InactividadDonaciones crit30 = crearCriterio(30);
    InactividadDonaciones crit45 = crearCriterio(45);

    List<DonanteInactivo> inactivos =
        gestor.procesarInactividad(List.of(crit30, crit45), List.of(inactivo));

    assertEquals(1, inactivos.size());
    assertEquals(50, inactivos.getFirst().diasInactivo());
  }

  @Test
  void gestorDeInactivos_conListasNulasOVacias_deberiaRetornarListaVacia() {
    GestorDeInactivos gestor = new GestorDeInactivos();

    assertTrue(gestor.procesarInactividad(null, null).isEmpty());
    assertTrue(gestor.procesarInactividad(List.of(), List.of()).isEmpty());
  }
}
