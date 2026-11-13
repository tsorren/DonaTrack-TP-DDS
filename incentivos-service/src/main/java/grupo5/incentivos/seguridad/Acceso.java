package grupo5.incentivos.seguridad;

/**
 * Expresiones de {@code @PreAuthorize} de Incentivos, según la matriz de permisos (§7). El ADMIN
 * pasa siempre; el donante, por rol, permiso y pertenencia. {@code donanteId} es el {@code
 * personaId} del donante (identidad compartida), así que la pertenencia es el id de la ruta.
 */
public final class Acceso {

  public static final String ADMIN = "hasRole('ADMIN')";
  public static final String ADMIN_O_SERVICIO = "hasAnyRole('ADMIN', 'SERVICIO')";
  public static final String DONANTE = ADMIN + " or hasRole('DONANTE') and ";
  public static final String SU_DONANTE = "principal.puedeOperarSobre(#donanteId)";

  /** Ver lo propio: perfil, ascensos, métricas, misiones, insignias y posición en el ranking. */
  public static final String VER_SUS_INCENTIVOS =
      DONANTE + "hasAuthority('VER_INCENTIVOS') and " + SU_DONANTE;

  /** Ver el ranking general: no tiene dueño, así que no pide pertenencia. */
  public static final String VER_RANKING = DONANTE + "hasAuthority('VER_INCENTIVOS')";

  public static final String CONFIGURAR_SUS_INSIGNIAS =
      DONANTE + "hasAuthority('CONFIGURAR_INSIGNIAS') and " + SU_DONANTE;

  private Acceso() {}
}
