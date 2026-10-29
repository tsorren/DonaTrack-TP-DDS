package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.categorias.CategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
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
  private INecesidadesRepository necesidadesRepositoryMock;
  private IItemDonacionNormalizadoRepository itemsNormalizadosRepositoryMock;
  private CategoriaMapper categoriaMapper;
  private SubcategoriaMapper subcategoriaMapper;
  private CategoriasService categoriasService;

  @BeforeEach
  void setUp() {
    categoriasRepositoryMock = mock(ICategoriasRepository.class);
    subcategoriasRepositoryMock = mock(ISubcategoriasRepository.class);
    necesidadesRepositoryMock = mock(INecesidadesRepository.class);
    itemsNormalizadosRepositoryMock = mock(IItemDonacionNormalizadoRepository.class);
    categoriaMapper = new CategoriaMapper();
    subcategoriaMapper = new SubcategoriaMapper(categoriaMapper, categoriasRepositoryMock);

    categoriasService =
        new CategoriasService(
            categoriasRepositoryMock,
            subcategoriasRepositoryMock,
            necesidadesRepositoryMock,
            itemsNormalizadosRepositoryMock,
            categoriaMapper,
            subcategoriaMapper);
  }

  @Test
  void eliminar_conUnaSubcategoriaEnUso_responde409YNoBorraNada() {
    Categoria categoria = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    Subcategoria libre = new Subcategoria(categoria.getId(), "Arroz");
    Subcategoria enUso = new Subcategoria(categoria.getId(), "Fideos");
    UUID id = categoria.getId();
    when(categoriasRepositoryMock.findById(id)).thenReturn(Optional.of(categoria));
    when(subcategoriasRepositoryMock.findAll()).thenReturn(List.of(libre, enUso));
    when(necesidadesRepositoryMock.existsBySubcategoriaId(enUso.getId())).thenReturn(true);

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> categoriasService.eliminar(id));

    assertEquals(ErrorCatalog.SUBCATEGORIA_EN_USO, ex.getError());
    verify(subcategoriasRepositoryMock, never()).delete(any());
    verify(categoriasRepositoryMock, never()).delete(any());
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
