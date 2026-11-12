package grupo5.donaciones.models.repositories.listados;

/**
 * Búsqueda de entidades beneficiarias por razón social (HB-15), ordenada por razón social. Con
 * {@code soloActivas}, las de baja no aparecen (el listado público).
 */
public record CriterioEntidades(String nombre, boolean soloActivas, Paginado paginado) {}
