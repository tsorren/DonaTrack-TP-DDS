package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.categorias.CategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface ICategoriasController {
  ResponseEntity<CategoriaOutputDTO> crear(CategoriaInputDTO dto);

  ResponseEntity<CategoriaOutputDTO> eliminar(UUID id);

  ResponseEntity<CategoriaOutputDTO> modificar(UUID id, CategoriaInputDTO dto);

  ResponseEntity<List<CategoriaOutputDTO>> obtenerTodas();

  ResponseEntity<CategoriaOutputDTO> obtener(UUID id);
}
