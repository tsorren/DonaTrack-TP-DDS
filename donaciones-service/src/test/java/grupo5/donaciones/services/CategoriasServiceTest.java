package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.categorias.CategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.impl.CategoriasService;
import grupo5.donaciones.services.mappers.CategoriaMapper;
import grupo5.donaciones.services.mappers.SubcategoriaMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoriasServiceTest {

  private ICategoriasRepository categoriasRepositoryMock;
  private ISubcategoriasRepository subcategoriasRepositoryMock;
  private CategoriaMapper categoriaMapperMock;
  private SubcategoriaMapper subcategoriaMapperMock;
  private CategoriasService categoriasService;

  @BeforeEach
  void setUp() {
    categoriasRepositoryMock = mock(ICategoriasRepository.class);
    subcategoriasRepositoryMock = mock(ISubcategoriasRepository.class);
    categoriaMapperMock = mock(CategoriaMapper.class);
    subcategoriaMapperMock = mock(SubcategoriaMapper.class);

    categoriasService =
        new CategoriasService(
            categoriasRepositoryMock,
            subcategoriasRepositoryMock,
            categoriaMapperMock,
            subcategoriaMapperMock);
  }

  @Test
  void crear_DeberiaMapearYGuardar() {
    CategoriaInputDTO input = new CategoriaInputDTO("Alimentos", false, true, Unidad.KILOGRAMO);
    Categoria entity = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    CategoriaOutputDTO output =
        new CategoriaOutputDTO(
            entity.getId(), "Alimentos", false, true, Unidad.KILOGRAMO, List.of());

    when(categoriaMapperMock.toEntity(input)).thenReturn(entity);
    when(categoriaMapperMock.toOutputDTO(eq(entity), any())).thenReturn(output);

    CategoriaOutputDTO result = categoriasService.crear(input);

    assertNotNull(result);
    assertEquals("Alimentos", result.nombre());
    verify(categoriasRepositoryMock, times(1)).save(entity);
  }

  @Test
  void obtenerPorId_DeberiaLanzarExcepcion_CuandoNoExiste() {
    UUID id = UUID.randomUUID();
    when(categoriasRepositoryMock.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> categoriasService.obtener(id));
  }

  @Test
  void obtenerPorId_DeberiaRetornarCategoria_CuandoExiste() {
    UUID id = UUID.randomUUID();
    Categoria entity = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    CategoriaOutputDTO output =
        new CategoriaOutputDTO(id, "Alimentos", false, true, Unidad.KILOGRAMO, List.of());

    when(categoriasRepositoryMock.findById(id)).thenReturn(Optional.of(entity));
    when(categoriaMapperMock.toOutputDTO(eq(entity), any())).thenReturn(output);

    CategoriaOutputDTO result = categoriasService.obtener(id);

    assertNotNull(result);
    assertEquals("Alimentos", result.nombre());
  }
}
