package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.impl.AcumuladosDeAsignacion;
import grupo5.donaciones.services.impl.CancelacionDeEntregas;
import grupo5.donaciones.services.impl.NecesidadesService;
import grupo5.donaciones.services.logistica.ILogisticaBroker;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * La lectura de necesidades por lista de ids (SPEC-11, G2, lote {@code g2-relaciones}): la carga
 * por lotes de {@code Donacion.necesidad}. Una sola lectura al repositorio para todos los ids y la
 * vista pública de cada una, aunque ya no esté vigente.
 */
class NecesidadesPublicasPorIdsServiceTest {

  private INecesidadesRepository repositorio;
  private NecesidadesService servicio;

  @BeforeEach
  void setUp() {
    repositorio = mock(INecesidadesRepository.class);
    IDonacionesIndependientesRepository independientes =
        mock(IDonacionesIndependientesRepository.class);
    servicio =
        new NecesidadesService(
            repositorio,
            mock(IEntidadesBeneficiariasRepository.class),
            mock(ISubcategoriasRepository.class),
            mock(ICategoriasRepository.class),
            new AcumuladosDeAsignacion(independientes),
            new CancelacionDeEntregas(independientes, mock(ILogisticaBroker.class)));
  }

  @Test
  void leeTodasEnUnaSolaLecturaYDevuelveLaVistaPublica_aunqueNoEstenVigentes() {
    UUID entidad = UUID.randomUUID();
    UUID subcategoria = UUID.randomUUID();
    NecesidadExtraordinaria dadaDeBaja = NecesidadMother.extraordinaria(entidad, subcategoria, 10);
    dadaDeBaja.desactivar();
    NecesidadRecurrente recurrente = NecesidadMother.recurrenteSemanal(entidad, subcategoria, 4);
    Set<UUID> ids = Set.of(dadaDeBaja.getId(), recurrente.getId());
    when(repositorio.findAllById(any())).thenReturn(List.of(dadaDeBaja, recurrente));

    List<NecesidadPublicaDTO> publicas = servicio.buscarPublicasPorIds(ids);

    verify(repositorio, times(1)).findAllById(ids);
    assertEquals(2, publicas.size());
    assertTrue(
        publicas.contains(
            new NecesidadPublicaDTO(
                dadaDeBaja.getId(),
                entidad,
                subcategoria,
                dadaDeBaja.getDescripcion(),
                10,
                "EXTRAORDINARIA",
                null)));
    NecesidadPublicaDTO deLaRecurrente =
        publicas.stream().filter(n -> n.id().equals(recurrente.getId())).findFirst().orElseThrow();
    assertEquals("RECURRENTE", deLaRecurrente.tipo());
    assertNotNull(deLaRecurrente.fechaFin());
  }

  @Test
  void sinIds_noLeeElRepositorio() {
    assertEquals(List.of(), servicio.buscarPublicasPorIds(Set.of()));

    verify(repositorio, never()).findAllById(any());
  }
}
