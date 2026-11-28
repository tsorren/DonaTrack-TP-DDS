package grupo5.donaciones.graphql.donaciones;

import grupo5.donaciones.dto.donacionesIndependientes.FiltroDonacionesIndependientes;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * El {@code input FiltroDonaciones} del esquema: los filtros de {@link
 * FiltroDonacionesIndependientes} y el texto de búsqueda. {@code donanteId()} y {@code entidadId()}
 * los lee además la regla de acceso ({@code Acceso.LISTAR_DONACIONES_INDEPENDIENTES}).
 *
 * <p>Los ids y las fechas se guardan como llegan y se leen al pedirlos: así un id que no es UUID o
 * una fecha que no es {@code AAAA-MM-DD} da {@code ERR-CSR-003} (un {@link
 * IllegalArgumentException}) y no un error de enlace de argumentos, que sería {@code ERR-CSR-500}.
 */
public final class FiltroDonaciones {

  private final String donanteId;
  private final String entidadId;
  private final List<TipoEstadoDonacion> estado;
  private final String necesidadId;
  private final String subcategoriaId;
  private final String categoriaId;
  private final String desde;
  private final String hasta;
  private final String venceHasta;
  private final String id;
  private final String texto;

  /** Los nombres de los parámetros son los campos del {@code input}: así los enlaza GraphQL. */
  @SuppressWarnings("java:S107")
  public FiltroDonaciones(
      String donanteId,
      String entidadId,
      List<TipoEstadoDonacion> estado,
      String necesidadId,
      String subcategoriaId,
      String categoriaId,
      String desde,
      String hasta,
      String venceHasta,
      String id,
      String texto) {
    this.donanteId = donanteId;
    this.entidadId = entidadId;
    this.estado = estado == null ? null : List.copyOf(estado);
    this.necesidadId = necesidadId;
    this.subcategoriaId = subcategoriaId;
    this.categoriaId = categoriaId;
    this.desde = desde;
    this.hasta = hasta;
    this.venceHasta = venceHasta;
    this.id = id;
    this.texto = texto;
  }

  public UUID donanteId() {
    return uuid(donanteId);
  }

  public UUID entidadId() {
    return uuid(entidadId);
  }

  public List<TipoEstadoDonacion> estado() {
    return estado;
  }

  public UUID necesidadId() {
    return uuid(necesidadId);
  }

  public UUID subcategoriaId() {
    return uuid(subcategoriaId);
  }

  public UUID categoriaId() {
    return uuid(categoriaId);
  }

  public LocalDate desde() {
    return fecha(desde);
  }

  public LocalDate hasta() {
    return fecha(hasta);
  }

  public LocalDate venceHasta() {
    return fecha(venceHasta);
  }

  public UUID id() {
    return uuid(id);
  }

  public String texto() {
    return texto;
  }

  /** Los filtros que entiende {@code obtenerConFiltros}; el texto va aparte, como en REST. */
  public FiltroDonacionesIndependientes deServicio() {
    return new FiltroDonacionesIndependientes(
        estado(),
        subcategoriaId(),
        categoriaId(),
        donanteId(),
        entidadId(),
        desde(),
        hasta(),
        necesidadId(),
        id(),
        venceHasta());
  }

  private static UUID uuid(String valor) {
    return valor == null ? null : UUID.fromString(valor);
  }

  private static LocalDate fecha(String valor) {
    return valor == null ? null : LocalDate.parse(valor);
  }
}
