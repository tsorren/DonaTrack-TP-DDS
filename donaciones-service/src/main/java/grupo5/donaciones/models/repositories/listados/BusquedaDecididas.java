package grupo5.donaciones.models.repositories.listados;

import java.time.LocalDate;
import java.util.UUID;

/**
 * La búsqueda de Todas las decididas (R11-35); un campo nulo no filtra. {@code q}: sin mayúsculas
 * ni tildes, en el nombre de la organización de la necesidad, lo que pide la necesidad y la
 * descripción de las donaciones que la cubrían. {@code categoriaId}: la de la necesidad. {@code
 * decididaPor}: el {@code sub} de quien decidió; {@link #SISTEMA} junta las de {@code SISTEMA} y
 * las que no tienen autor. {@code desde} y {@code hasta}: días inclusivos sobre la fecha de la
 * decisión; las que no la tienen no entran.
 */
public record BusquedaDecididas(
    String q, UUID categoriaId, String decididaPor, LocalDate desde, LocalDate hasta) {

  /** El autor de las decididas sin cuenta; como filtro, junta también las que no tienen autor. */
  public static final String SISTEMA = "SISTEMA";

  private static final BusquedaDecididas NINGUNA =
      new BusquedaDecididas(null, null, null, null, null);

  public static BusquedaDecididas ninguna() {
    return NINGUNA;
  }

  /** Si {@code decididaPor} pide las de sistema o sin autor. */
  public boolean deSistema() {
    return SISTEMA.equals(decididaPor);
  }
}
