package grupo5.donaciones.services;

import grupo5.donaciones.dto.categorias.*;
import java.util.List;
import java.util.UUID;

public interface ISubcategoriasService {
  SubcategoriaOutputDTO crear(SubcategoriaInputDTO dto);

  SubcategoriaOutputDTO eliminar(UUID id);

  SubcategoriaOutputDTO modificar(UUID id, SubcategoriaInputDTO dto);

  List<SubcategoriaOutputDTO> obtenerTodas();

  SubcategoriaOutputDTO obtener(UUID id);

  SubcategoriaOutputDTO agregarAlias(UUID id, AliasSubcategoriaInputDTO dto);

  SubcategoriaOutputDTO quitarAlias(UUID id, UUID idAlias);
}
