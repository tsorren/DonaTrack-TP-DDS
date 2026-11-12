package grupo5.donaciones.services;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.publico.EntidadParaDonanteDTO;
import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import java.util.UUID;

public interface IEntidadBeneficiariaService {
  /** Registro idempotente: {@code creado = false} si la jurídica ya era entidad. */
  ResultadoRegistro<EntidadBeneficiariaOutputDTO> crearEntidad(EntidadBeneficiariaInputDTO input);

  EntidadBeneficiariaOutputDTO obtenerEntidad(UUID id);

  /**
   * Entidades cuya razón social contiene {@code nombre} (sin distinguir mayúsculas ni tildes; nulo
   * no filtra), ordenadas por razón social, de a una página resuelta en la base ({@code page} desde
   * 0, por defecto 0; {@code size} entre 1 y 100, por defecto 20; R11-01: sin parámetros ya no
   * devuelve todas). El total cuenta las que cumplen el filtro.
   */
  Pagina<EntidadBeneficiariaOutputDTO> buscar(String nombre, Integer page, Integer size);

  /**
   * Lo mismo que {@link #buscar} sobre las entidades activas, con el DTO público (D26): sin
   * documento, dirección, contacto ni representantes. Con {@code categoriaId} (R11-12), solo las
   * que piden algo de esa categoría: una necesidad activa y no vencida de una de sus subcategorías.
   */
  Pagina<EntidadPublicaDTO> buscarPublicas(
      String nombre, UUID categoriaId, Integer page, Integer size);

  /**
   * Las entidades activas vistas por un donante (R11-12): la misma búsqueda que {@link
   * #buscarPublicas} y, con {@code soloDonadas}, solo las que recibieron alguna donación suya, en
   * cualquier estado. Cada fila dice si ese donante ya le donó. Si el donante no existe, 404.
   */
  Pagina<EntidadParaDonanteDTO> buscarParaDonante(
      UUID donanteId,
      String nombre,
      UUID categoriaId,
      boolean soloDonadas,
      Integer page,
      Integer size);

  EntidadBeneficiariaOutputDTO actualizarEntidad(UUID id, EntidadBeneficiariaInputDTO input);

  /** Baja lógica con cascada a sus necesidades. Idempotente. */
  void eliminarEntidad(UUID id);

  /** Indica si esa jurídica es una entidad beneficiaria activa. */
  boolean esEntidadActiva(UUID juridicaId);

  /** Da de baja a la entidad de esa jurídica si existe; si no es entidad no hace nada. */
  void darDeBajaSiExiste(UUID juridicaId);
}
