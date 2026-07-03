package grupo5.donaciones.config;

import grupo5.donaciones.models.entities.personas.TipoDocumento;
import java.util.UUID;

/**
 * Datos fijos de la persona administradora del sistema.
 *
 * <p>El {@link #ID_ADMIN} es el mismo UUID literal usado en la clase espejo de
 * notificaciones-service ({@code grupo5.notificaciones.config.AdminConstantes}). Ambos servicios
 * siembran una persona administradora con este id al arrancar, de forma independiente, para que los
 * eventos de logística/donaciones puedan referenciar a la admin sin depender de que la
 * sincronización asincrónica entre servicios haya llegado a tiempo.
 */
public final class AdminConstantes {

  private AdminConstantes() {
    throw new IllegalStateException("Utility class");
  }

  public static final UUID ID_ADMIN = UUID.fromString("00000000-0000-0000-0000-000000000001");
  public static final TipoDocumento TIPO_DOCUMENTO_ADMIN = TipoDocumento.DNI;
  public static final String DOCUMENTO_ADMIN = "ADMIN-000000";
  public static final String NOMBRE_ADMIN = "Administradora";
  public static final String APELLIDO_ADMIN = "Sistema";
  public static final String EMAIL_ADMIN = "admin@donatrack.com";
}
