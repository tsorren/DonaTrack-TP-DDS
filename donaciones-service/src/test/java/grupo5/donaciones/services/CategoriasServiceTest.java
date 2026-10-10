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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoriasServiceTest {

  private ICategoriasRepository categoriasRepositoryMock;
  private ISubcategoriasRepository subcategoriasRepositoryMock;
  private CategoriaMapper categoriaMapper;
  private SubcategoriaMapper subcategoriaMapper;
  private CategoriasService categoriasService;

  @BeforeEach
  void setUp() {
    categoriasRepositoryMock = mock(ICategoriasRepository.class);
    subcategoriasRepositoryMock = mock(ISubcategoriasRepository.class);
    categoriaMapper = new CategoriaMapper();
    subcategoriaMapper = new SubcategoriaMapper(categoriaMapper, categoriasRepositoryMock);

    categoriasService =
        new CategoriasService(
            categoriasRepositoryMock,
            subcategoriasRepositoryMock,
            categoriaMapper,
            subcategoriaMapper);
  }

  @Test
  void crear_DeberiaMapearYGuardar() {
    CategoriaInputDTO input = new CategoriaInputDTO("Alimentos", false, true, Unidad.KILOGRAMO);

    CategoriaOutputDTO result = categoriasService.crear(input);

    assertNotNull(result);
    assertEquals("Alimentos", result.nombre());
    verify(categoriasRepositoryMock, times(1)).save(any(Categoria.class));
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

    when(categoriasRepositoryMock.findById(id)).thenReturn(Optional.of(entity));

    CategoriaOutputDTO result = categoriasService.obtener(id);

    assertNotNull(result);
    assertEquals("Alimentos", result.nombre());
  }
}
