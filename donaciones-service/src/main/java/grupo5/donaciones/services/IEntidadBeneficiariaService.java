package grupo5.donaciones.services;

import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import java.util.List;
import java.util.UUID;

public interface IEntidadBeneficiariaService {
  /** Registro idempotente: {@code creado = false} si la jurídica ya era entidad. */
  ResultadoRegistro<EntidadBeneficiariaOutputDTO> crearEntidad(EntidadBeneficiariaInputDTO input);

  EntidadBeneficiariaOutputDTO obtenerEntidad(UUID id);

  List<EntidadBeneficiariaOutputDTO> obtenerTodas();

  EntidadBeneficiariaOutputDTO actualizarEntidad(UUID id, EntidadBeneficiariaInputDTO input);

  /** Baja lógica con cascada a sus necesidades. Idempotente. */
  void eliminarEntidad(UUID id);

  /** Da de baja a la entidad de esa jurídica si existe; si no es entidad no hace nada. */
  void darDeBajaSiExiste(UUID juridicaId);
}
