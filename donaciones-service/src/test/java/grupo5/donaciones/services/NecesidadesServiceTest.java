package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadesServiceTest {

  private INecesidadesRepository necesidadRepositoryMock;
  private NecesidadesService necesidadesService;

  @BeforeEach
  void setUp() {
    necesidadRepositoryMock = mock(INecesidadesRepository.class);

    necesidadesService = new NecesidadesService(necesidadRepositoryMock);
  }

  @Test
  void guardar_DeberiaPersistirNecesidadRecurrenteCorrectamente() {
    NecesidadDTO inputDto = new NecesidadDTO();
    inputDto.setId(UUID.randomUUID());
    inputDto.setTipo("RECURRENTE");
    inputDto.setSubcategoriaNombre("Alimentos");
    inputDto.setCantidadNecesitada(10);
    inputDto.setDescripcion("Fideos para el comedor");
    inputDto.setFechaInicio(LocalDate.of(2026, 6, 1));
    inputDto.setFechaFin(LocalDate.of(2026, 7, 1)); // un mes después

    when(necesidadRepositoryMock.save(eq(inputDto.getId()), any(NecesidadDTO.class)))
        .thenReturn(inputDto);

    NecesidadDTO resultado = necesidadesService.guardar(inputDto);

    assertNotNull(resultado);
    assertEquals("RECURRENTE", resultado.getTipo());
    assertFalse(resultado.getEstaSatisfecha(), "Al crearse, no debería estar satisfecha");
    verify(necesidadRepositoryMock, times(1)).save(eq(inputDto.getId()), any(NecesidadDTO.class));
  }

  @Test
  void guardar_DeberiaPersistirNecesidadExtraordinariaCorrectamente() {
    NecesidadDTO inputDto = new NecesidadDTO();
    inputDto.setId(UUID.randomUUID());
    inputDto.setTipo("EXTRAORDINARIA");
    inputDto.setSubcategoriaNombre("Ropa");
    inputDto.setCantidadNecesitada(5);
    inputDto.setDescripcion("Campañas de invierno");

    when(necesidadRepositoryMock.save(eq(inputDto.getId()), any(NecesidadDTO.class)))
        .thenReturn(inputDto);

    NecesidadDTO resultado = necesidadesService.guardar(inputDto);

    assertNotNull(resultado);
    assertEquals("EXTRAORDINARIA", resultado.getTipo());
    verify(necesidadRepositoryMock, times(1)).save(eq(inputDto.getId()), any(NecesidadDTO.class));
  }

  @Test
  void obtenerPorId_DeberiaRetornarDto_CuandoElIdExiste() {
    UUID idExistente = UUID.randomUUID();
    NecesidadDTO dtoEsperado = new NecesidadDTO();
    dtoEsperado.setId(idExistente);
    dtoEsperado.setDescripcion("Test por ID");

    when(necesidadRepositoryMock.findById(idExistente)).thenReturn(Optional.of(dtoEsperado));

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
        },
        "Debería lanzar RecursoNoEncontradoException si el repo devuelve un Optional vacío");
  }
}
