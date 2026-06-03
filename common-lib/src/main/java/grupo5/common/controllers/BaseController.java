package grupo5.common.controllers;

import grupo5.common.responses.ApiResponse;
import grupo5.common.services.BaseService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public abstract class BaseController<ID, Recurso> {
  private final BaseService<ID, Recurso> service;

  protected BaseController(BaseService<ID, Recurso> service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<Recurso>>> findAll() {
    List<Recurso> recursos = service.findAll();
    return ok("Elementos encontrados", recursos);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<Recurso>> findById(@PathVariable ID id) {
    Recurso recurso = service.findById(id);
    return ok("Elemento encontrado", recurso);
  }

  protected <Respuesta> ResponseEntity<ApiResponse<Respuesta>> ok(
      String message, Respuesta data) {
    return ResponseEntity.ok(ApiResponse.success(message, data));
  }

  protected ResponseEntity<ApiResponse<Void>> ok(String message) {
    return ResponseEntity.ok(ApiResponse.success(message));
  }

  protected <Respuesta> ResponseEntity<ApiResponse<Respuesta>> created(
      String message, Respuesta data) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(message, data));
  }

  protected BaseService<ID, Recurso> service() {
    return service;
  }
}
