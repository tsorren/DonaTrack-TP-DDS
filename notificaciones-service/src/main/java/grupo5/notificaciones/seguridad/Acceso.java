package grupo5.notificaciones.seguridad;

/**
 * Expresiones de {@code @PreAuthorize} de Notificaciones, según la matriz de permisos (§8). El
 * ADMIN pasa siempre; el donante y la entidad, por rol, permiso y pertenencia. Las notificaciones
 * se dirigen a la persona en cuyo nombre se opera, así que la pertenencia es su {@code personaId}.
 */
public final class Acceso {

  public static final String ADMIN = "hasRole('ADMIN')";
  public static final String ADMIN_O_SERVICIO = "hasAnyRole('ADMIN', 'SERVICIO')";
  public static final String DONANTE_O_ENTIDAD =
      ADMIN + " or hasAnyRole('DONANTE', 'ENTIDAD') and ";
  public static final String SU_PERSONA = "principal.puedeOperarSobre(#personaId)";
  public static final String SU_NOTIFICACION =
      "principal.puedeOperarSobre(@duenioDe.notificacion(#id))";

  /** Lo que se dirige a su persona: el historial, el conteo y marcar todas como leídas. */
  public static final String VER_SUS_NOTIFICACIONES =
      DONANTE_O_ENTIDAD + "hasAuthority('VER_NOTIFICACIONES') and " + SU_PERSONA;

  /** Una notificación puntual: el dueño sale de la propia notificación, no de la ruta. */
  public static final String MARCAR_SU_NOTIFICACION =
      DONANTE_O_ENTIDAD + "hasAuthority('VER_NOTIFICACIONES') and " + SU_NOTIFICACION;

  private Acceso() {}
}
