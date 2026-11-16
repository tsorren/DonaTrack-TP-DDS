package grupo5.notificaciones.dto;

import java.util.List;

/**
 * Los parámetros de la bandeja de una persona tal como llegan por la URL (R11-14, spec
 * R11-p1-notificaciones §3.2). Los valida {@code NotificacionService}: sin {@code page} ni {@code
 * size} se pide la página 0 de 20; un valor fuera de rango da 400 con {@code ERR-CSR-003}.
 *
 * @param leida {@code true} o {@code false}, solo esas; sin valor, todas
 * @param q el texto buscado en el mensaje, hasta 100 caracteres; vacío, sin búsqueda
 * @param tipo solo las de esos tipos de evento (repetible)
 * @param excluirTipo todas menos las de esos tipos (repetible); las que no tienen tipo entran
 * @param orden {@code recientes} (por defecto) o {@code antiguas}
 * @param page desde 0
 * @param size de 1 a 100
 */
public record FiltroBandeja(
    Boolean leida,
    String q,
    List<String> tipo,
    List<String> excluirTipo,
    String orden,
    Integer page,
    Integer size) {

  /** Solo el filtro de lectura: la primera página de 20, de la más nueva a la más vieja. */
  public static FiltroBandeja deLectura(Boolean leida) {
    return new FiltroBandeja(leida, null, null, null, null, null, null);
  }
}
