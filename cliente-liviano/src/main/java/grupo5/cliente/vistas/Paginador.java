package grupo5.cliente.vistas;

/**
 * El paginador provisorio de una lista (R11-01, hasta el componente de R11-11): "‹ Anterior ·
 * Página N de M · Siguiente ›". {@code nombre} es el de la navegación para los lectores ("Páginas
 * de donantes"); {@code anterior} y {@code siguiente}, las direcciones de las páginas vecinas, con
 * la búsqueda y los filtros que había, o null en el extremo que no sigue. {@code cambio}: si se
 * llegó a esta página desde el paginador ({@code ?pagina=} en la dirección): el foco va al título
 * de la lista.
 */
public record Paginador(
    String nombre, int pagina, int paginas, String anterior, String siguiente, boolean cambio) {}
