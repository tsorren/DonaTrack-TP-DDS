package grupo5.donaciones.services;

import grupo5.donaciones.dto.categorias.CategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import java.util.List;
import java.util.UUID;

public interface ICategoriasService {
  CategoriaOutputDTO crear(CategoriaInputDTO dto);

  CategoriaOutputDTO eliminar(UUID id);

  CategoriaOutputDTO modificar(UUID id, CategoriaInputDTO dto);

  List<CategoriaOutputDTO> obtenerTodas();

  CategoriaOutputDTO obtener(UUID id);
}
