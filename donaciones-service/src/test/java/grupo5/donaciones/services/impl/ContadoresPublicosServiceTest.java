package grupo5.donaciones.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donaciones.outputs.ContadoresPublicosDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContadoresPublicosServiceTest {

  private final IDonantesRepository donantes = mock(IDonantesRepository.class);
  private final IDonacionesRepository donaciones = mock(IDonacionesRepository.class);
  private final IDonacionesIndependientesRepository independientes =
      mock(IDonacionesIndependientesRepository.class);
  private final IEntidadBeneficiariaService entidades = mock(IEntidadBeneficiariaService.class);
  private final Instant ahora = Instant.parse("2026-11-08T21:30:00Z");

  private final ContadoresPublicosService service =
      new ContadoresPublicosService(
          donantes, donaciones, independientes, entidades, Clock.fixed(ahora, ZoneOffset.UTC));

  @Test
  void calcular_cuentaSoloLosDonantesActivosQueDonaron() {
    UUID activoQueDono = UUID.randomUUID();
    UUID inactivoQueDono = UUID.randomUUID();
    UUID activoSinDonar = UUID.randomUUID();
    when(donantes.findAll())
        .thenReturn(
            List.of(
                new Donante(activoQueDono, true),
                new Donante(inactivoQueDono, false),
                new Donante(activoSinDonar, true)));
    when(donaciones.idsDeDonantes()).thenReturn(Set.of(activoQueDono, inactivoQueDono));
    when(independientes.contarEnEstado(TipoEstadoDonacion.ENTREGADA)).thenReturn(7L);
    when(entidades.buscarPublicas(null, 0, 1)).thenReturn(new Pagina<>(List.of(), 4));

    ContadoresPublicosDTO contadores = service.calcular();

    assertEquals(1, contadores.donantesActivos());
    assertEquals(7, contadores.donacionesRealizadas());
    assertEquals(4, contadores.beneficiariosActivos());
    assertEquals(OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC), contadores.calculadoEn());
  }

  @Test
  void calcular_sinDatos_devuelveCeros() {
    when(donantes.findAll()).thenReturn(List.of());
    when(donaciones.idsDeDonantes()).thenReturn(Set.of());
    when(entidades.buscarPublicas(null, 0, 1)).thenReturn(new Pagina<>(List.of(), 0));

    ContadoresPublicosDTO contadores = service.calcular();

    assertEquals(0, contadores.donantesActivos());
    assertEquals(0, contadores.donacionesRealizadas());
    assertEquals(0, contadores.beneficiariosActivos());
  }
}
