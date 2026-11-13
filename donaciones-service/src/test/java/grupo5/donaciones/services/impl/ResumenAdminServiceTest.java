package grupo5.donaciones.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.donaciones.dto.donaciones.outputs.ResumenAdminDTO;
import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.models.repositories.listados.PorVencer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResumenAdminServiceTest {

  private static final Instant AHORA = Instant.parse("2026-11-11T15:00:00Z");
  private static final LocalDate HOY = LocalDate.of(2026, 11, 11);

  private final IDonacionesIndependientesRepository independientes =
      mock(IDonacionesIndependientesRepository.class);
  private final IDonantesRepository donantes = mock(IDonantesRepository.class);
  private final IEntidadesBeneficiariasRepository entidades =
      mock(IEntidadesBeneficiariasRepository.class);
  private final IPropuestasRepository propuestas = mock(IPropuestasRepository.class);

  private final ResumenAdminService service =
      new ResumenAdminService(
          independientes, donantes, entidades, propuestas, Clock.fixed(AHORA, ZoneOffset.UTC));

  @BeforeEach
  void setUp() {
    when(independientes.buscarPorVencer(HOY, HOY.plusDays(7))).thenReturn(PorVencer.ninguna());
  }

  @Test
  void calcular_traeCadaCifraDeSuConteo() {
    when(independientes.contarPorEstado())
        .thenReturn(Map.of(TipoEstadoDonacion.EN_DEPOSITO, 12L, TipoEstadoDonacion.ENTREGADA, 30L));
    when(independientes.contarQuePasaronA(
            TipoEstadoDonacion.ENTREGADA,
            LocalDateTime.of(2026, 11, 1, 0, 0),
            LocalDateTime.of(2026, 12, 1, 0, 0)))
        .thenReturn(4L);
    when(donantes.count()).thenReturn(50L);
    when(donantes.contarActivos()).thenReturn(45L);
    when(entidades.contarActivas()).thenReturn(6L);
    when(propuestas.contarEnEstado(EstadoPropuesta.PENDIENTE)).thenReturn(3L);

    ResumenAdminDTO resumen = service.calcular();

    assertEquals(new ResumenAdminDTO.EntregadasEnElMes("2026-11", 4), resumen.entregadasEnElMes());
    assertEquals(new ResumenAdminDTO.Donantes(50, 45), resumen.donantes());
    assertEquals(6, resumen.entidadesActivas());
    assertEquals(3, resumen.propuestasPendientes());
    assertEquals(OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC), resumen.calculadoEn());
    // Una consulta de conteo por cifra: nada recorre la lista de donaciones
    verify(independientes, never()).findAll();
    verify(donantes, never()).findAll();
  }

  @Test
  void calcular_losOchoEstadosSiempreEstan_enElOrdenDelCiclo_conCeroSiNoHay() {
    when(independientes.contarPorEstado()).thenReturn(Map.of(TipoEstadoDonacion.ENTREGADA, 2L));

    Map<String, Long> porEstado = service.calcular().donacionesPorEstado();

    assertEquals(
        List.of(TipoEstadoDonacion.values()).stream().map(Enum::name).toList(),
        List.copyOf(porEstado.keySet()));
    assertEquals(2L, porEstado.get("ENTREGADA"));
    assertEquals(0L, porEstado.get("EN_DEPOSITO"));
  }

  @Test
  void calcular_porVencer_informaLaProximaConSuDescripcion() {
    DonacionIndependiente proxima = DonacionIndependienteMother.enDeposito(1);
    LocalDate vence = HOY.plusDays(2);
    when(independientes.contarPorEstado()).thenReturn(Map.of());
    when(independientes.buscarPorVencer(HOY, HOY.plusDays(7)))
        .thenReturn(new PorVencer(3, proxima.getId(), vence));
    when(independientes.findById(proxima.getId())).thenReturn(Optional.of(proxima));

    ResumenAdminDTO.PorVencer porVencer = service.calcular().porVencer();

    assertEquals(7, porVencer.dias());
    assertEquals(3, porVencer.cantidad());
    assertEquals(proxima.getId(), porVencer.proxima().id());
    assertEquals(proxima.getDescripcion().trim(), porVencer.proxima().descripcion());
    assertEquals(vence, porVencer.proxima().fechaVencimiento());
  }

  @Test
  void calcular_sinNadaPorVencer_noTraeProxima() {
    when(independientes.contarPorEstado()).thenReturn(Map.of());

    ResumenAdminDTO.PorVencer porVencer = service.calcular().porVencer();

    assertEquals(0, porVencer.cantidad());
    assertNull(porVencer.proxima());
  }
}
