package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.donaciones.models.repositories.listados.Paginado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Arma las dos consultas SQL de una página (R11-01): los ids del tramo, en orden, y el total con
 * los mismos filtros. Solo se agregan las condiciones de los filtros presentes, así ningún
 * parámetro llega nulo (PostgreSQL no infiere su tipo). El {@code FROM} lleva solo uniones uno a
 * uno; lo que mira colecciones va en un {@code EXISTS}, para que el conteo no repita filas.
 */
final class ConsultaListado {

  private final String desde;
  private final List<String> condiciones = new ArrayList<>();
  private final Map<String, Object> parametros = new LinkedHashMap<>();

  ConsultaListado(String desde) {
    this.desde = desde;
  }

  ConsultaListado donde(String condicion) {
    condiciones.add(condicion);
    return this;
  }

  ConsultaListado donde(String condicion, String parametro, Object valor) {
    condiciones.add(condicion);
    parametros.put(parametro, valor);
    return this;
  }

  /** Agrega la condición solo si el valor no es nulo. */
  ConsultaListado siHay(Object valor, String condicion, String parametro) {
    return valor == null ? this : donde(condicion, parametro, valor);
  }

  /** Agrega la condición solo si la colección no está vacía. */
  ConsultaListado siHay(Collection<?> valores, String condicion, String parametro) {
    return valores == null || valores.isEmpty() ? this : donde(condicion, parametro, valores);
  }

  /**
   * "Contiene", sin mayúsculas ni tildes, sobre la expresión SQL dada, contra el parámetro {@code
   * :parametro}. Con {@code %}, {@code _} y {@code \} escapados.
   */
  static String contiene(String expresion, String parametro) {
    return sinTildes(expresion) + " LIKE " + sinTildes(":" + parametro) + " ESCAPE '\\'";
  }

  /**
   * La expresión en minúsculas y sin tildes. Con el diccionario calificado, {@code unaccent} no
   * depende del {@code search_path} de la conexión (V21 crea la extensión en el esquema).
   */
  static String sinTildes(String expresion) {
    return "lower(donaciones.unaccent('donaciones.unaccent', " + expresion + "))";
  }

  /** El patrón de {@link #contiene(String, String)} para el texto buscado. */
  static String patron(String q) {
    String escapado = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + escapado + "%";
  }

  /** Los ids del tramo pedido, en el orden dado (que tiene que desempatar por id). */
  List<UUID> ids(EntityManager em, String columnaId, String orden, Paginado paginado) {
    Query consulta =
        em.createNativeQuery(
            "SELECT "
                + columnaId
                + " "
                + desde
                + where()
                + " ORDER BY "
                + orden
                + " LIMIT :limite OFFSET :desplazamiento");
    aplicar(consulta);
    consulta.setParameter("limite", paginado.tamanio());
    consulta.setParameter("desplazamiento", paginado.desplazamiento());
    List<?> filas = consulta.getResultList();
    return filas.stream().map(ConsultaListado::uuid).toList();
  }

  long total(EntityManager em) {
    Query consulta = em.createNativeQuery("SELECT count(*) " + desde + where());
    aplicar(consulta);
    return ((Number) consulta.getSingleResult()).longValue();
  }

  private String where() {
    return condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
  }

  private void aplicar(Query consulta) {
    parametros.forEach(consulta::setParameter);
  }

  static UUID uuid(Object valor) {
    return valor instanceof UUID id ? id : UUID.fromString(valor.toString());
  }

  /** Reordena las filas leídas por lote según el orden de los ids de la página. */
  static <T> List<T> enOrden(List<UUID> ids, Collection<T> filas, Function<T, UUID> id) {
    Map<UUID, T> porId = new LinkedHashMap<>();
    filas.forEach(fila -> porId.put(id.apply(fila), fila));
    return ids.stream().map(porId::get).filter(Objects::nonNull).toList();
  }
}
