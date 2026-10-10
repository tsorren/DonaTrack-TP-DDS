package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.ICategoriasController;
import grupo5.donaciones.dto.categorias.CategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import grupo5.donaciones.services.ICategoriasService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class CategoriasController implements ICategoriasController {

  private final ICategoriasService categoriasService;

  public CategoriasController(ICategoriasService categoriasService) {
    this.categoriasService = categoriasService;
  }

  @Override
  @PostMapping
  public ResponseEntity<CategoriaOutputDTO> crear(@Valid @RequestBody CategoriaInputDTO dto) {
    CategoriaOutputDTO categoria = categoriasService.crear(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(categoria);
  }

  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<CategoriaOutputDTO> eliminar(@PathVariable UUID id) {
    CategoriaOutputDTO eliminada = categoriasService.eliminar(id);
    return ResponseEntity.ok(eliminada);
  }

  @Override
  @PutMapping("/{id}")
  public ResponseEntity<CategoriaOutputDTO> modificar(
      @PathVariable UUID id, @Valid @RequestBody CategoriaInputDTO dto) {
    CategoriaOutputDTO modificada = categoriasService.modificar(id, dto);
    return ResponseEntity.ok(modificada);
  }

  @Override
  @GetMapping
  public ResponseEntity<List<CategoriaOutputDTO>> obtenerTodas() {
    List<CategoriaOutputDTO> categorias = categoriasService.obtenerTodas();
    return ResponseEntity.ok(categorias);
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<CategoriaOutputDTO> obtener(@PathVariable UUID id) {
    CategoriaOutputDTO categoria = categoriasService.obtener(id);
    return ResponseEntity.ok(categoria);
  }
}
