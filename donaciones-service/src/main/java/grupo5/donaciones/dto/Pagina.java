package grupo5.donaciones.dto;

import java.util.List;

/**
 * Un tramo de un listado: los elementos pedidos y el total de los que cumplen el filtro (el
 * controller lo informa en el header {@code X-Total-Count}).
 */
public record Pagina<T>(List<T> elementos, int total) {}
