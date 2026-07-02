package grupo5.donaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import grupo5.donaciones.dto.categorias.AliasSubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubcategoriaMapperTest {

  @Mock private CategoriaMapper categoriaMapper;
  @Mock private ICategoriasRepository categoriasRepository;

  @InjectMocks private SubcategoriaMapper mapper;

  @Test
  void toEntity_debeMapearCorrectamente() {
    Categoria categoria = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    SubcategoriaInputDTO input =
        new SubcategoriaInputDTO(
            "Fideos", categoria.getId(), List.of(new AliasSubcategoriaInputDTO("tallarines")));

    Subcategoria entity = mapper.toEntity(input, categoria);

    assertNotNull(entity);
    assertEquals("Fideos", entity.getNombre());
    assertEquals(categoria.getId(), entity.getCategoriaId());
    assertEquals(1, entity.getAliases().size());
    assertEquals("tallarines", entity.getAliases().get(0).alias());
  }

  @Test
  void toOutputDTO_debeMapearCorrectamente() {
    Categoria categoria = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    Subcategoria entity = new Subcategoria(categoria.getId(), "Fideos");
    entity.agregarAlias("tallarines");

    CategoriaOutputDTO catDto =
        new CategoriaOutputDTO(
            categoria.getId(), "Alimentos", false, true, Unidad.KILOGRAMO, List.of());

    when(categoriasRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
    when(categoriaMapper.toOutputDTO(categoria)).thenReturn(catDto);

    SubcategoriaOutputDTO output = mapper.toOutputDTO(entity);

    assertNotNull(output);
    assertEquals(entity.getId(), output.id());
    assertEquals("Fideos", output.nombre());
    assertEquals(catDto, output.categoria());
    assertEquals(1, output.aliases().size());
    assertEquals("tallarines", output.aliases().get(0).alias());
  }
}
