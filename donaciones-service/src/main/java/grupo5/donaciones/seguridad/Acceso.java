package grupo5.donaciones.seguridad;

/** Expresiones de {@code @PreAuthorize}: el ADMIN pasa siempre y el resto, por rol y permiso. */
public final class Acceso {

  public static final String ADMIN = "hasRole('ADMIN')";
  public static final String ADMIN_O_SERVICIO = "hasAnyRole('ADMIN', 'SERVICIO')";
  public static final String CON_SESION = "isAuthenticated()";
  public static final String PUBLICO = "permitAll()";
  public static final String DONANTE = ADMIN + " or hasRole('DONANTE') and ";
  public static final String ENTIDAD = ADMIN + " or hasRole('ENTIDAD') and ";
  public static final String DONANTE_O_ENTIDAD =
      ADMIN + " or hasAnyRole('DONANTE', 'ENTIDAD') and ";
  public static final String SU_ID = "principal.puedeOperarSobre(#id)";
  public static final String SU_DONACION = "principal.puedeOperarSobre(@duenioDe.donacion(#id))";
  public static final String SU_DONACION_INDEPENDIENTE =
      "principal.puedeOperarSobre(@duenioDe.donacionIndependiente(#id))";
  public static final String SU_NECESIDAD = "principal.puedeOperarSobre(@duenioDe.necesidad(#id))";

  private Acceso() {}
}
