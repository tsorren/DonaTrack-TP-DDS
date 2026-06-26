package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.categorias.AliasSubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.impl.SubcategoriasService;
import grupo5.donaciones.services.mappers.SubcategoriaMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubcategoriasServiceTest {

  private ISubcategoriasRepository subcategoriasRepositoryMock;
  private ICategoriasRepository categoriasRepositoryMock;
  private SubcategoriaMapper subcategoriaMapperMock;
  private SubcategoriasService subcategoriasService;

  @BeforeEach
  void setUp() {
    subcategoriasRepositoryMock = mock(ISubcategoriasRepository.class);
    categoriasRepositoryMock = mock(ICategoriasRepository.class);
    subcategoriaMapperMock = mock(SubcategoriaMapper.class);

    subcategoriasService =
        new SubcategoriasService(
            subcategoriasRepositoryMock, categoriasRepositoryMock, subcategoriaMapperMock);
  }

  @Test
  void crear_DeberiaMapearYGuardar() {
    UUID categoriaId = UUID.randomUUID();
    Categoria categoria = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    SubcategoriaInputDTO input = new SubcategoriaInputDTO("Fideos", categoriaId, List.of());
    Subcategoria entity = new Subcategoria(categoria, "Fideos");
    SubcategoriaOutputDTO output =
        new SubcategoriaOutputDTO(entity.getId(), "Fideos", null, List.of());

    when(categoriasRepositoryMock.findById(categoriaId)).thenReturn(Optional.of(categoria));
    when(subcategoriaMapperMock.toEntity(input, categoria)).thenReturn(entity);
    when(subcategoriaMapperMock.toOutputDTO(entity)).thenReturn(output);

    SubcategoriaOutputDTO result = subcategoriasService.crear(input);

    assertNotNull(result);
    assertEquals("Fideos", result.nombre());
    verify(subcategoriasRepositoryMock, times(1)).save(entity);
  }

  @Test
  void agregarAlias_DeberiaAgregarYGuardar() {
    UUID id = UUID.randomUUID();
    Categoria categoria = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    Subcategoria entity = new Subcategoria(categoria, "Fideos");
    AliasSubcategoriaInputDTO input = new AliasSubcategoriaInputDTO("tallarines");
    SubcategoriaOutputDTO output =
        new SubcategoriaOutputDTO(entity.getId(), "Fideos", null, List.of());

    when(subcategoriasRepositoryMock.findById(id)).thenReturn(Optional.of(entity));
    when(subcategoriaMapperMock.toOutputDTO(entity)).thenReturn(output);

    SubcategoriaOutputDTO result = subcategoriasService.agregarAlias(id, input);

    assertNotNull(result);
    assertTrue(entity.tieneAlias("tallarines"));
    verify(subcategoriasRepositoryMock, times(1)).save(entity);
  }
}
