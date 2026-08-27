package grupo5.incentivos.dto;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.fixtures.EventoDonacionMother;
import grupo5.incentivos.fixtures.MisionMother;
import grupo5.incentivos.fixtures.RankingMensualMother;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.insignias.InsigniaGanada;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DTOsAndMappersTest {

  @Test
  void donanteRegistradoDTO_desde_deberiaMapearCorrectamente() {
    UUID id = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(id);

    DonanteRegistradoDTO dto = DonanteRegistradoDTO.desde(donante);

    assertEquals(id, dto.donanteId());
    assertEquals(donante.getCategoria().name(), dto.categoria());
  }

  @Test
  void insigniaDTO_desde_deberiaMapearInsigniaEInsigniaGanada() {
    Insignia plantilla = new Insignia("Insignia Plantilla", "Descripcion", "/icono.png");
    InsigniaDTO dtoPlantilla = InsigniaDTO.desde(plantilla);

    assertNotNull(dtoPlantilla);
    assertEquals(plantilla.nombre(), dtoPlantilla.nombre());
    assertTrue(dtoPlantilla.visible());
    assertNull(dtoPlantilla.fechaObtenida());

    InsigniaGanada ganada =
        new InsigniaGanada(
            "Insignia Ganada", "Desc Ganada", "/icono2.png", false, LocalDate.of(2026, 5, 10));
    InsigniaDTO dtoGanada = InsigniaDTO.desde(ganada);

    assertNotNull(dtoGanada);
    assertEquals(ganada.nombre(), dtoGanada.nombre());
    assertFalse(dtoGanada.visible());
    assertEquals(ganada.fechaObtenida(), dtoGanada.fechaObtenida());

    assertNull(InsigniaDTO.desde((Insignia) null));
    assertNull(InsigniaDTO.desde((InsigniaGanada) null));
  }

  @Test
  void metricasDonanteDTO_desde_deberiaMapearMetricasConYMisionActiva() {
    UUID id = UUID.randomUUID();
    MisionRacha racha = MisionMother.rachaColaborador(3);
    DonanteIncentivos donante = DonanteIncentivosMother.conMisiones(id, List.of(racha));

    MetricasDonanteDTO dto = MetricasDonanteDTO.desde(donante, 1, 0, Map.of("2026-05", 2L));

    assertNotNull(dto);
    assertEquals(id, dto.donanteId());
    assertEquals(1, dto.posicionEnRanking());
    assertNotNull(dto.misionActiva());
    assertEquals(racha.getNombre(), dto.misionActiva().nombre());
    assertEquals(racha.getObjetivo(), dto.misionActiva().objetivo());
  }

  @Test
  void misionDTO_desde_deberiaMapearCorrectamente() {
    MisionRacha racha =
        MisionMother.rachaConInsignia(
            grupo5.incentivos.models.entities.donante.CategoriaDonante.COLABORADOR,
            2,
            "Racha Bronce");

    MisionDTO dto = MisionDTO.desde(racha);

    assertNotNull(dto);
    assertEquals(racha.getNombre(), dto.nombre());
    assertEquals(racha.getObjetivo(), dto.objetivo());
    assertNotNull(dto.insignia());
    assertEquals(racha.getInsignia().nombre(), dto.insignia().nombre());
  }

  @Test
  void rankingMensualDTO_desde_deberiaMapearRankingYEntradas() {
    YearMonth mayo = YearMonth.of(2026, 5);
    RankingMensual ranking = RankingMensualMother.conNEntradas(mayo, 4);

    RankingMensualDTO dto = RankingMensualDTO.desde(ranking);

    assertNotNull(dto);
    assertEquals(mayo.toString(), dto.periodo());
    assertEquals(4, dto.entradas().size());
    assertEquals(3, dto.podio().size());
    assertEquals(1, dto.podio().getFirst().posicion());
  }

  @Test
  void resumenSistemaDTO_y_requestToEvento_deberianFuncionarCorrectamente() {
    ResumenSistemaDTO resumen =
        new ResumenSistemaDTO(10, 5, 3, 20L, 4L, Map.of("COLABORADOR", 10L), Map.of("2026-05", 5L));

    assertEquals(10, resumen.totalDonantes());
    assertEquals(5, resumen.donantesMesActual());
    assertEquals(3, resumen.donantesMesAnterior());

    UUID donacionId = UUID.randomUUID();
    EventoDonacion evento = EventoDonacionMother.enFecha(LocalDate.of(2026, 6, 17));

    assertNotNull(evento);
    assertEquals(LocalDate.of(2026, 6, 17), evento.getFecha());
  }
}
