package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.impl.AcumuladosDeAsignacion;
import grupo5.donaciones.services.impl.NecesidadesService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadesServiceTest {

  private INecesidadesRepository necesidadRepositoryMock;
  private IEntidadesBeneficiariasRepository entidadesBeneficiariasRepositoryMock;
  private ISubcategoriasRepository subcategoriaRepositoryMock;
  private IDonacionesIndependientesRepository donacionesIndependientesRepositoryMock;
  private NecesidadesService necesidadesService;

  @BeforeEach
  void setUp() {
    necesidadRepositoryMock = mock(INecesidadesRepository.class);
    entidadesBeneficiariasRepositoryMock = mock(IEntidadesBeneficiariasRepository.class);
    subcategoriaRepositoryMock = mock(ISubcategoriasRepository.class);
    donacionesIndependientesRepositoryMock = mock(IDonacionesIndependientesRepository.class);

    necesidadesService =
        new NecesidadesService(
            necesidadRepositoryMock,
            entidadesBeneficiariasRepositoryMock,
            subcategoriaRepositoryMock,
            new AcumuladosDeAsignacion(donacionesIndependientesRepositoryMock));
  }

  @Test
  void guardar_DeberiaPersistirNecesidadRecurrenteCorrectamente() {
    UUID subcategoriaId = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();

    NecesidadDTO inputDto =
        new NecesidadDTO(
            UUID.randomUUID(),
            "RECURRENTE",
            entidadId,
            subcategoriaId,
            10,
            "Fideos para el comedor",
            false,
            LocalDate.of(2026, java.time.Month.JUNE, 1),
            LocalDate.of(2026, java.time.Month.JULY, 1));

    Subcategoria subcategoriaMock = mock(Subcategoria.class);
    EntidadBeneficiaria entidadMock = mock(EntidadBeneficiaria.class);
    when(entidadMock.estaActivo()).thenReturn(true);

    when(subcategoriaRepositoryMock.findById(subcategoriaId))
        .thenReturn(Optional.of(subcategoriaMock));
    when(entidadesBeneficiariasRepositoryMock.findById(entidadId))
        .thenReturn(Optional.of(entidadMock));

    NecesidadDTO resultado = necesidadesService.guardar(inputDto);

    assertNotNull(resultado);
    assertEquals("RECURRENTE", resultado.tipo());
    verify(necesidadRepositoryMock, times(1)).save(any(Necesidad.class));
  }

  @Test
  void guardar_conEntidadDeBaja_lanzaEntidadInactivaYNoGuarda() {
    UUID entidadId = UUID.randomUUID();
    NecesidadDTO inputDto =
        new NecesidadDTO(
            UUID.randomUUID(),
            "EXTRAORDINARIA",
            entidadId,
            UUID.randomUUID(),
            5,
            "Mantas",
            false,
            null,
            null);
    EntidadBeneficiaria entidadDeBaja = mock(EntidadBeneficiaria.class);
    when(entidadDeBaja.estaActivo()).thenReturn(false);
    when(subcategoriaRepositoryMock.findById(inputDto.idSubcategoria()))
        .thenReturn(Optional.of(mock(Subcategoria.class)));
    when(entidadesBeneficiariasRepositoryMock.findById(entidadId))
        .thenReturn(Optional.of(entidadDeBaja));

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> necesidadesService.guardar(inputDto));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_INACTIVA, ex.getError());
    verify(necesidadRepositoryMock, never()).save(any(Necesidad.class));
  }

  @Test
  void guardar_DeberiaPersistirNecesidadExtraordinariaCorrectamente() {
    UUID subcategoriaId = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();

    NecesidadDTO inputDto =
        new NecesidadDTO(
            UUID.randomUUID(),
            "EXTRAORDINARIA",
            entidadId,
            subcategoriaId,
            5,
            "Campañas de invierno",
            false,
            LocalDate.of(2026, java.time.Month.JUNE, 1),
            null);

    Subcategoria subcategoriaMock = mock(Subcategoria.class);
    EntidadBeneficiaria entidadMock = mock(EntidadBeneficiaria.class);
    when(entidadMock.estaActivo()).thenReturn(true);

    when(subcategoriaRepositoryMock.findById(subcategoriaId))
        .thenReturn(Optional.of(subcategoriaMock));
    when(entidadesBeneficiariasRepositoryMock.findById(entidadId))
        .thenReturn(Optional.of(entidadMock));

    NecesidadDTO resultado = necesidadesService.guardar(inputDto);

    assertNotNull(resultado);
    assertEquals("EXTRAORDINARIA", resultado.tipo());
    verify(necesidadRepositoryMock, times(1)).save(any(Necesidad.class));
  }

  @Test
  void obtenerPorId_DeberiaRetornarDto_CuandoElIdExiste() {
    UUID idExistente = UUID.randomUUID();
    Necesidad necesidadMock = mock(Necesidad.class);
    NecesidadDTO dtoEsperado =
        new NecesidadDTO(
            idExistente,
            "EXTRAORDINARIA",
            UUID.randomUUID(),
            UUID.randomUUID(),
            5,
            "Test por ID",
            false,
            LocalDate.of(2026, java.time.Month.JUNE, 18),
            null);

    when(necesidadMock.toDTO(anyInt())).thenReturn(dtoEsperado);
    when(necesidadRepositoryMock.findById(idExistente)).thenReturn(Optional.of(necesidadMock));

    NecesidadDTO resultado = necesidadesService.obtenerPorId(idExistente);

    assertNotNull(resultado);
    assertEquals(idExistente, resultado.id());
    assertEquals("Test por ID", resultado.descripcion());
  }

  @Test
  void obtenerPorId_DeberiaLanzarExcepcion_CuandoElIdNoExiste() {
    UUID idInexistente = UUID.randomUUID();
    when(necesidadRepositoryMock.findById(idInexistente)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNoEncontradoException.class,
        () -> {
          necesidadesService.obtenerPorId(idInexistente);
        });
  }

  // ===================== listarPublicas (HB-08) =====================

  @Test
  void listarPublicas_devuelveSoloLasActivasNoSatisfechasConElDtoPublico() {
    UUID entidadId = UUID.randomUUID();
    UUID subcategoriaId = UUID.randomUUID();
    NecesidadExtraordinaria vigente = NecesidadMother.extraordinaria(entidadId, subcategoriaId, 10);
    NecesidadExtraordinaria satisfecha =
        NecesidadMother.extraordinaria(entidadId, subcategoriaId, 3);
    NecesidadExtraordinaria inactiva = NecesidadMother.extraordinaria(entidadId, subcategoriaId, 5);
    inactiva.desactivar();
    when(necesidadRepositoryMock.findAll()).thenReturn(List.of(vigente, satisfecha, inactiva));
    when(donacionesIndependientesRepositoryMock.sumarCantidadAsignada(anyMap()))
        .thenReturn(Map.of(vigente.getId(), 4, satisfecha.getId(), 3, inactiva.getId(), 0));

    List<NecesidadPublicaDTO> publicas = necesidadesService.listarPublicas(null);

    assertEquals(
        List.of(
            new NecesidadPublicaDTO(
                vigente.getId(),
                entidadId,
                subcategoriaId,
                vigente.getDescripcion(),
                10,
                "EXTRAORDINARIA",
                null)),
        publicas);
  }

  @Test
  void listarPublicas_porEntidad_filtraLasDeEsaEntidad() {
    UUID entidadId = UUID.randomUUID();
    NecesidadExtraordinaria propia =
        NecesidadMother.extraordinaria(entidadId, UUID.randomUUID(), 2);
    NecesidadExtraordinaria ajena =
        NecesidadMother.extraordinaria(UUID.randomUUID(), UUID.randomUUID(), 2);
    when(necesidadRepositoryMock.findAll()).thenReturn(List.of(propia, ajena));

    assertEquals(
        List.of(propia.getId()),
        necesidadesService.listarPublicas(entidadId).stream()
            .map(NecesidadPublicaDTO::id)
            .toList());
  }

  @Test
  void listarPublicas_recurrente_informaElFinDelPeriodoActual() {
    UUID entidadId = UUID.randomUUID();
    NecesidadRecurrente semanal =
        NecesidadMother.recurrenteSemanal(entidadId, UUID.randomUUID(), 4);
    when(necesidadRepositoryMock.findAll()).thenReturn(List.of(semanal));

    List<NecesidadPublicaDTO> publicas = necesidadesService.listarPublicas(entidadId);

    assertEquals(1, publicas.size());
    assertEquals("RECURRENTE", publicas.getFirst().tipo());
    assertEquals(semanal.obtenerPeriodoActual().fechaFin(), publicas.getFirst().fechaFin());
    assertNotNull(publicas.getFirst().fechaFin());
  }
}
