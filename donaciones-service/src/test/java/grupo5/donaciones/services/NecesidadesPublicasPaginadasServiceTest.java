package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.TipoNecesidad;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioNecesidades;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import grupo5.donaciones.services.impl.AcumuladosDeAsignacion;
import grupo5.donaciones.services.impl.CancelacionDeEntregas;
import grupo5.donaciones.services.impl.NecesidadesService;
import grupo5.donaciones.services.logistica.ILogisticaBroker;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * La lectura paginada de necesidades vigentes (SPEC-11, G2, lote {@code g2-necesidades}): pasa los
 * filtros y la página al repositorio y devuelve el DTO público, el mismo de {@code listarPublicas}.
 */
class NecesidadesPublicasPaginadasServiceTest {

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
  void pasaLosFiltrosYLaPaginaAlRepositorio_conElDiaDeHoy() {
    UUID entidad = UUID.randomUUID();
    UUID categoria = UUID.randomUUID();
    UUID subcategoria = UUID.randomUUID();
    when(repositorio.buscarVigentes(any())).thenReturn(new Tramo<>(List.of(), 0));

    servicio.buscarPublicas(entidad, categoria, subcategoria, TipoNecesidad.RECURRENTE, 2, 15);

    ArgumentCaptor<CriterioNecesidades> criterio =
        ArgumentCaptor.forClass(CriterioNecesidades.class);
    verify(repositorio).buscarVigentes(criterio.capture());
    assertEquals(
        new CriterioNecesidades(
            entidad,
            categoria,
            subcategoria,
            TipoNecesidad.RECURRENTE,
            LocalDate.now(ZoneId.systemDefault()),
            new Paginado(2, 15)),
        criterio.getValue());
  }

  @Test
  void devuelveElDtoPublicoDeCadaUnaYElTotal() {
    UUID entidad = UUID.randomUUID();
    UUID subcategoria = UUID.randomUUID();
    NecesidadExtraordinaria extraordinaria =
        NecesidadMother.extraordinaria(entidad, subcategoria, 10);
    NecesidadRecurrente recurrente = NecesidadMother.recurrenteSemanal(entidad, subcategoria, 4);
    List<Necesidad> pagina = List.of(extraordinaria, recurrente);
    when(repositorio.buscarVigentes(any())).thenReturn(new Tramo<>(pagina, 7));

    Pagina<NecesidadPublicaDTO> resultado = servicio.buscarPublicas(null, null, null, null, 0, 2);

    assertEquals(7, resultado.total());
    assertEquals(
        new NecesidadPublicaDTO(
            extraordinaria.getId(),
            entidad,
            subcategoria,
            extraordinaria.getDescripcion(),
            10,
            "EXTRAORDINARIA",
            null),
        resultado.elementos().get(0));
    NecesidadPublicaDTO deLaRecurrente = resultado.elementos().get(1);
    assertEquals("RECURRENTE", deLaRecurrente.tipo());
    assertNotNull(deLaRecurrente.fechaFin());
    assertEquals(recurrente.obtenerPeriodoActual().fechaFin(), deLaRecurrente.fechaFin());
  }

  @Test
  void unaPaginaNegativaOUnTamanioMenorAUnoEsArgumentoInvalido() {
    ValidationException negativa =
        assertThrows(
            ValidationException.class,
            () -> servicio.buscarPublicas(null, null, null, null, -1, 5));
    ValidationException vacia =
        assertThrows(
            ValidationException.class, () -> servicio.buscarPublicas(null, null, null, null, 0, 0));

    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, negativa.getError());
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, vacia.getError());
    verify(repositorio, never()).buscarVigentes(any());
  }
}
