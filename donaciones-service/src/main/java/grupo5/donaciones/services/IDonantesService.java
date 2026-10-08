package grupo5.donaciones.services;

import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import java.util.List;
import java.util.UUID;

public interface IDonantesService {
  /** Registro idempotente: {@code creado = false} si la persona ya era donante. */
  ResultadoRegistro<DonanteOutputDTO> crearDonante(DonanteInputDTO dto);

  List<DonanteOutputDTO> listarDonantesPorContacto(String canal);

  DonanteOutputDTO obtenerPorId(UUID id);

  /**
   * Registra a la persona como donante solo si nunca lo fue. Si ya lo era (activo o de baja) no
   * modifica nada, y devuelve cuál de los casos se dio.
   */
  EstadoRegistroDonante registrarSiNoExiste(UUID personaId);

  /** Baja lógica. Idempotente: si ya estaba de baja no hace nada. */
  void eliminarDonante(UUID id);

  /** Da de baja al donante de esa persona si existe; si la persona no es donante no hace nada. */
  void darDeBajaSiExiste(UUID personaId);
}
