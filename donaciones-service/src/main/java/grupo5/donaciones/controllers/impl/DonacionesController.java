package grupo5.donaciones.controllers.impl;

import static grupo5.donaciones.seguridad.Acceso.ADMIN;
import static grupo5.donaciones.seguridad.Acceso.DONANTE;
import static grupo5.donaciones.seguridad.Acceso.SU_DONACION;

import grupo5.donaciones.controllers.IDonacionesController;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.services.IDonacionesService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize(ADMIN)
@RequestMapping("/api/donaciones")
public class DonacionesController implements IDonacionesController {

  private final IDonacionesService service;

  public DonacionesController(IDonacionesService service) {
    this.service = service;
  }

  @Override
  @PreAuthorize(
      DONANTE
          + "hasAuthority('REGISTRAR_DONACIONES') and principal.puedeOperarSobre(#dto.idDonante())")
  @PostMapping
  public ResponseEntity<DonacionOutputDTO> cargarDonacion(
      @Valid @RequestBody DonacionInputDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.cargarDonacion(dto));
  }

  @Override
  @GetMapping
  public ResponseEntity<List<DonacionOutputDTO>> listarDonaciones() {
    return ResponseEntity.ok(service.listarDonaciones());
  }

  @Override
  @PreAuthorize(DONANTE + "hasAuthority('VER_DONACIONES') and " + SU_DONACION)
  @GetMapping("/{id}")
  public ResponseEntity<DonacionOutputDTO> obtenerDonacion(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(service.obtenerDonacion(id));
  }
}
