package grupo5.donaciones.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.donaciones.dto.donaciones.outputs.ContadoresPublicosDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.impl.DonantesRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.EntidadesBeneficiariasRepositoryEnMemoria;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContadoresPublicosServiceTest {

  private final IDonacionesRepository donaciones = mock(IDonacionesRepository.class);
  private final DonantesRepositoryEnMemoria donantes =
      new DonantesRepositoryEnMemoria(null, donaciones, null, null);
  private final IDonacionesIndependientesRepository independientes =
      mock(IDonacionesIndependientesRepository.class);
  private final EntidadesBeneficiariasRepositoryEnMemoria entidades =
      new EntidadesBeneficiariasRepositoryEnMemoria();
  private final Instant ahora = Instant.parse("2026-11-08T21:30:00Z");

  private final ContadoresPublicosService service =
      new ContadoresPublicosService(
          donantes, independientes, entidades, Clock.fixed(ahora, ZoneOffset.UTC));

  @Test
  void calcular_cuentaSoloLosDonantesActivosQueDonaron() {
    UUID activoQueDono = UUID.randomUUID();
    UUID inactivoQueDono = UUID.randomUUID();
    UUID activoSinDonar = UUID.randomUUID();
    donantes.save(new Donante(activoQueDono, true));
    donantes.save(new Donante(inactivoQueDono, false));
    donantes.save(new Donante(activoSinDonar, true));
    when(donaciones.idsDeDonantes()).thenReturn(Set.of(activoQueDono, inactivoQueDono));
    when(independientes.contarEnEstado(TipoEstadoDonacion.ENTREGADA)).thenReturn(7L);
    for (int i = 0; i < 4; i++) {
      entidades.save(new EntidadBeneficiaria(UUID.randomUUID()));
    }
    EntidadBeneficiaria deBaja = new EntidadBeneficiaria(UUID.randomUUID());
    deBaja.darDeBaja();
    entidades.save(deBaja);

    ContadoresPublicosDTO contadores = service.calcular();

    assertEquals(1, contadores.donantesActivos());
    assertEquals(7, contadores.donacionesRealizadas());
    assertEquals(4, contadores.beneficiariosActivos());
    assertEquals(OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC), contadores.calculadoEn());
    // R11-01: cada cifra es un conteo; ninguna recorre la lista de donaciones independientes
    verify(independientes, never()).findAll();
  }

  @Test
  void calcular_sinDatos_devuelveCeros() {
    when(donaciones.idsDeDonantes()).thenReturn(Set.of());

    ContadoresPublicosDTO contadores = service.calcular();

    assertEquals(0, contadores.donantesActivos());
    assertEquals(0, contadores.donacionesRealizadas());
    assertEquals(0, contadores.beneficiariosActivos());
  }
}
