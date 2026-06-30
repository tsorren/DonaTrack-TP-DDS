package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.ICategoriasController;
import grupo5.donaciones.dto.categorias.CategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import grupo5.donaciones.services.ICategoriasService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${donatrack.routes.donaciones.categorias-base}")
public class CategoriasController implements ICategoriasController {

  private final ICategoriasService categoriasService;

  public CategoriasController(ICategoriasService categoriasService) {
    this.categoriasService = categoriasService;
  }

  @Override
  @PostMapping
  public ResponseEntity<CategoriaOutputDTO> crear(@RequestBody CategoriaInputDTO dto) {
    CategoriaOutputDTO creada = categoriasService.crear(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(creada);
  }

  @Override
  @DeleteMapping("${donatrack.routes.donaciones.categorias-id}")
  public ResponseEntity<CategoriaOutputDTO> eliminar(@PathVariable UUID id) {
    CategoriaOutputDTO eliminada = categoriasService.eliminar(id);
    return ResponseEntity.ok(eliminada);
  }

  @Override
  @PutMapping("${donatrack.routes.donaciones.categorias-id}")
  public ResponseEntity<CategoriaOutputDTO> modificar(
      @PathVariable UUID id, @RequestBody CategoriaInputDTO dto) {
    CategoriaOutputDTO modificada = categoriasService.modificar(id, dto);
    return ResponseEntity.ok(modificada);
  }

  @Override
  @GetMapping
  public ResponseEntity<List<CategoriaOutputDTO>> obtenerTodas() {
    List<CategoriaOutputDTO> lista = categoriasService.obtenerTodas();
    return ResponseEntity.ok(lista);
  }

  @Override
  @GetMapping("${donatrack.routes.donaciones.categorias-id}")
  public ResponseEntity<CategoriaOutputDTO> obtener(@PathVariable UUID id) {
    CategoriaOutputDTO obtenida = categoriasService.obtener(id);
    return ResponseEntity.ok(obtenida);
  }
}
