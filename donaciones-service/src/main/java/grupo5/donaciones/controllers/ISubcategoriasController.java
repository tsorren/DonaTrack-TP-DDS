package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.categorias.AliasSubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface ISubcategoriasController {
  ResponseEntity<SubcategoriaOutputDTO> crear(SubcategoriaInputDTO dto);

  ResponseEntity<SubcategoriaOutputDTO> eliminar(UUID id);

  ResponseEntity<SubcategoriaOutputDTO> modificar(UUID id, SubcategoriaInputDTO dto);

  ResponseEntity<List<SubcategoriaOutputDTO>> obtenerTodas();

  ResponseEntity<SubcategoriaOutputDTO> obtener(UUID id);

  ResponseEntity<SubcategoriaOutputDTO> agregarAlias(UUID id, AliasSubcategoriaInputDTO dto);

  ResponseEntity<SubcategoriaOutputDTO> quitarAlias(UUID id, UUID idAlias);
}
