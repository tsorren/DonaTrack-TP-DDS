package grupo5.logistica.seguridad;

/**
 * Expresiones de {@code @PreAuthorize} de Logística, según la matriz de permisos (§6). El ADMIN
 * pasa siempre; el donante y la entidad, por rol, permiso y pertenencia. Una entrega es de su
 * beneficiaria y de su donante, y una ruta es de quien tenga una entrega en ella.
 */
public final class Acceso {

  public static final String ADMIN = "hasRole('ADMIN')";
  public static final String ADMIN_O_SERVICIO = "hasAnyRole('ADMIN', 'SERVICIO')";
  public static final String PUBLICO = "permitAll()";
  public static final String DONANTE = ADMIN + " or hasRole('DONANTE') and ";
  public static final String ENTIDAD = ADMIN + " or hasRole('ENTIDAD') and ";
  public static final String DONANTE_O_ENTIDAD =
      ADMIN + " or hasAnyRole('DONANTE', 'ENTIDAD') and ";

  public static final String SU_DONANTE = "principal.puedeOperarSobre(#donanteId)";
  public static final String SU_BENEFICIARIA = "principal.puedeOperarSobre(#idBeneficiaria)";
  public static final String SU_ENTREGA = "principal.puedeOperarSobre(@duenioDe.entrega(#id))";
  public static final String SU_DONACION =
      "principal.puedeOperarSobre(@duenioDe.donacion(#idDonacion))";
  public static final String SU_RUTA = "@duenioDe.ruta(#id, principal)";

  /** Una entrega propia de la entidad: consultarla y ver su historial o su foto. */
  public static final String VER_SU_ENTREGA =
      ENTIDAD + "hasAuthority('VER_ENTREGAS') and " + SU_ENTREGA;

  /** Confirmar o rechazar la recepción y adjuntar o subir las fotos de una entrega propia. */
  public static final String CONFIRMAR_SU_ENTREGA =
      ENTIDAD + "hasAuthority('CONFIRMAR_RECEPCION') and " + SU_ENTREGA;

  public static final String VER_EL_SEGUIMIENTO_DE_SU_DONACION =
      DONANTE + "hasAuthority('VER_ENTREGAS') and " + SU_DONACION;
  public static final String VER_EL_SEGUIMIENTO_DE_SU_DONANTE =
      DONANTE + "hasAuthority('VER_ENTREGAS') and " + SU_DONANTE;
  public static final String VER_EL_SEGUIMIENTO_DE_SU_BENEFICIARIA =
      ENTIDAD + "hasAuthority('VER_ENTREGAS') and " + SU_BENEFICIARIA;

  /** Una ruta que lleva una entrega de su organización. */
  public static final String VER_SU_RUTA =
      DONANTE_O_ENTIDAD + "hasAuthority('VER_RUTAS') and " + SU_RUTA;

  private Acceso() {}
}
