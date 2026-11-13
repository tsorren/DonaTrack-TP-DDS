package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EstadoOrganizacion;
import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface IEntidadBeneficiariaController {
  ResponseEntity<EntidadBeneficiariaOutputDTO> crearEntidad(EntidadBeneficiariaInputDTO entidad);

  ResponseEntity<EntidadBeneficiariaOutputDTO> obtenerEntidad(UUID id);

  ResponseEntity<List<EntidadBeneficiariaOutputDTO>> obtenerTodas(
      String nombre,
      EstadoOrganizacion estado,
      Boolean conNecesidades,
      String rubro,
      Integer page,
      Integer size);

  ResponseEntity<List<EntidadPublicaDTO>> obtenerPublicas(
      String nombre, UUID categoriaId, Integer page, Integer size);

  ResponseEntity<EntidadBeneficiariaOutputDTO> actualizarEntidad(
      UUID id, EntidadBeneficiariaInputDTO entidad);

  ResponseEntity<Void> eliminarEntidad(UUID id);
}
