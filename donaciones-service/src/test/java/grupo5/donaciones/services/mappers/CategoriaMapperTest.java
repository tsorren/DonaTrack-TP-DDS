package grupo5.donaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.dto.categorias.CategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategoriaMapperTest {

  private final CategoriaMapper mapper = new CategoriaMapper();

  @Test
  void toEntity_debeMapearCorrectamente() {
    CategoriaInputDTO input = new CategoriaInputDTO("Alimentos", false, true, Unidad.KILOGRAMO);

    Categoria entity = mapper.toEntity(input);

    assertNotNull(entity);
    assertNotNull(entity.getId());
    assertEquals("Alimentos", entity.getNombre());
    assertFalse(entity.getConUso());
    assertTrue(entity.getConVencimiento());
    assertEquals(Unidad.KILOGRAMO, entity.getTipoUnidad());
  }

  @Test
  void toOutputDTO_debeMapearCorrectamente() {
    Categoria entity = new Categoria("Ropa", true, false, Unidad.UNIDADES);

    CategoriaOutputDTO output = mapper.toOutputDTO(entity, List.of());

    assertNotNull(output);
    assertEquals(entity.getId(), output.id());
    assertEquals("Ropa", output.nombre());
    assertTrue(output.conUso());
    assertFalse(output.conVencimiento());
    assertEquals(Unidad.UNIDADES, output.unidad());
    assertTrue(output.subcategorias().isEmpty());
  }

  @Test
  void updateEntity_debeActualizarCampos() {
    Categoria entity = new Categoria("Muebles", true, false, Unidad.UNIDADES);
    CategoriaInputDTO input =
        new CategoriaInputDTO("Muebles Actualizados", false, true, Unidad.METRO);

    mapper.updateEntity(entity, input);

    assertEquals("Muebles Actualizados", entity.getNombre());
    assertFalse(entity.getConUso());
    assertTrue(entity.getConVencimiento());
    assertEquals(Unidad.METRO, entity.getTipoUnidad());
  }
}
