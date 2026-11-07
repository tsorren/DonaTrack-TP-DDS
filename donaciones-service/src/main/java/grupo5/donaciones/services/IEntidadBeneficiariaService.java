package grupo5.donaciones.services;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import java.util.List;
import java.util.UUID;

public interface IEntidadBeneficiariaService {
  /** Registro idempotente: {@code creado = false} si la jurídica ya era entidad. */
  ResultadoRegistro<EntidadBeneficiariaOutputDTO> crearEntidad(EntidadBeneficiariaInputDTO input);

  EntidadBeneficiariaOutputDTO obtenerEntidad(UUID id);

  List<EntidadBeneficiariaOutputDTO> obtenerTodas();

  /**
   * Entidades cuya razón social contiene {@code nombre} (sin distinguir mayúsculas; nulo no
   * filtra). Sin {@code page} ni {@code size} devuelve todas, en el orden de {@link
   * #obtenerTodas()}. Con alguno de los dos, ordena por razón social y devuelve ese tramo ({@code
   * page} desde 0, por defecto 0; {@code size} entre 1 y 100, por defecto 20). El total cuenta las
   * que cumplen el filtro.
   */
  Pagina<EntidadBeneficiariaOutputDTO> buscar(String nombre, Integer page, Integer size);

  /**
   * Lo mismo que {@link #buscar} sobre las entidades activas, con el DTO público (D26): sin
   * documento, dirección, contacto ni representantes.
   */
  Pagina<EntidadPublicaDTO> buscarPublicas(String nombre, Integer page, Integer size);

  EntidadBeneficiariaOutputDTO actualizarEntidad(UUID id, EntidadBeneficiariaInputDTO input);

  /** Baja lógica con cascada a sus necesidades. Idempotente. */
  void eliminarEntidad(UUID id);

  /** Indica si esa jurídica es una entidad beneficiaria activa. */
  boolean esEntidadActiva(UUID juridicaId);

  /** Da de baja a la entidad de esa jurídica si existe; si no es entidad no hace nada. */
  void darDeBajaSiExiste(UUID juridicaId);
}
