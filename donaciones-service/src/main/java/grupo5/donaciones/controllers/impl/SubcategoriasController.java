package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.ISubcategoriasController;
import grupo5.donaciones.dto.categorias.AliasSubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import grupo5.donaciones.services.ISubcategoriasService;
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
@RequestMapping("/api/subcategorias")
public class SubcategoriasController implements ISubcategoriasController {

  private final ISubcategoriasService subcategoriasService;

  public SubcategoriasController(ISubcategoriasService subcategoriasService) {
    this.subcategoriasService = subcategoriasService;
  }

  @Override
  @PostMapping
  public ResponseEntity<SubcategoriaOutputDTO> crear(@RequestBody SubcategoriaInputDTO dto) {
    SubcategoriaOutputDTO creada = subcategoriasService.crear(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(creada);
  }

  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<SubcategoriaOutputDTO> eliminar(@PathVariable UUID id) {
    SubcategoriaOutputDTO eliminada = subcategoriasService.eliminar(id);
    return ResponseEntity.ok(eliminada);
  }

  @Override
  @PutMapping("/{id}")
  public ResponseEntity<SubcategoriaOutputDTO> modificar(
      @PathVariable UUID id, @RequestBody SubcategoriaInputDTO dto) {
    SubcategoriaOutputDTO modificada = subcategoriasService.modificar(id, dto);
    return ResponseEntity.ok(modificada);
  }

  @Override
  @GetMapping
  public ResponseEntity<List<SubcategoriaOutputDTO>> obtenerTodas() {
    List<SubcategoriaOutputDTO> lista = subcategoriasService.obtenerTodas();
    return ResponseEntity.ok(lista);
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<SubcategoriaOutputDTO> obtener(@PathVariable UUID id) {
    SubcategoriaOutputDTO obtenida = subcategoriasService.obtener(id);
    return ResponseEntity.ok(obtenida);
  }

  @Override
  @PostMapping("/{id}/aliases")
  public ResponseEntity<SubcategoriaOutputDTO> agregarAlias(
      @PathVariable UUID id, @RequestBody AliasSubcategoriaInputDTO dto) {
    SubcategoriaOutputDTO actualizada = subcategoriasService.agregarAlias(id, dto);
    return ResponseEntity.ok(actualizada);
  }

  @Override
  @DeleteMapping("/{id}/aliases/{alias}")
  public ResponseEntity<SubcategoriaOutputDTO> quitarAlias(
      @PathVariable UUID id, @PathVariable String alias) {
    SubcategoriaOutputDTO actualizada = subcategoriasService.quitarAlias(id, alias);
    return ResponseEntity.ok(actualizada);
  }
}
