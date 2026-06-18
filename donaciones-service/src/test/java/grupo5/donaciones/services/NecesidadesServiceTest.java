package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.impl.NecesidadesService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadesServiceTest {

  private INecesidadesRepository necesidadRepositoryMock;
  private IEntidadesBeneficiariasRepository entidadesBeneficiariasRepositoryMock;
  private ISubcategoriasRepository subcategoriaRepositoryMock;
  private NecesidadesService necesidadesService;

  @BeforeEach
  void setUp() {
    necesidadRepositoryMock = mock(INecesidadesRepository.class);
    entidadesBeneficiariasRepositoryMock = mock(IEntidadesBeneficiariasRepository.class);
    subcategoriaRepositoryMock = mock(ISubcategoriasRepository.class);

    necesidadesService =
        new NecesidadesService(
            necesidadRepositoryMock,
            entidadesBeneficiariasRepositoryMock,
            subcategoriaRepositoryMock);
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

    when(subcategoriaRepositoryMock.findById(subcategoriaId))
        .thenReturn(Optional.of(subcategoriaMock));
    when(entidadesBeneficiariasRepositoryMock.findById(entidadId))
        .thenReturn(Optional.of(entidadMock));

    NecesidadDTO resultado = necesidadesService.guardar(inputDto);

    assertNotNull(resultado);
    assertEquals("RECURRENTE", resultado.getTipo());
    verify(necesidadRepositoryMock, times(1)).save(any(Necesidad.class));
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

    when(subcategoriaRepositoryMock.findById(subcategoriaId))
        .thenReturn(Optional.of(subcategoriaMock));
    when(entidadesBeneficiariasRepositoryMock.findById(entidadId))
        .thenReturn(Optional.of(entidadMock));

    NecesidadDTO resultado = necesidadesService.guardar(inputDto);

    assertNotNull(resultado);
    assertEquals("EXTRAORDINARIA", resultado.getTipo());
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

    when(necesidadMock.toDTO()).thenReturn(dtoEsperado);
    when(necesidadRepositoryMock.findById(idExistente)).thenReturn(Optional.of(necesidadMock));

    NecesidadDTO resultado = necesidadesService.obtenerPorId(idExistente);

    assertNotNull(resultado);
    assertEquals(idExistente, resultado.getId());
    assertEquals("Test por ID", resultado.getDescripcion());
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
}
