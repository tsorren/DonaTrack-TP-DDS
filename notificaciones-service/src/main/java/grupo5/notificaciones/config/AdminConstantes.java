package grupo5.notificaciones.config;

import java.util.UUID;

/**
 * Clase espejo de {@code grupo5.donaciones.config.AdminConstantes}. El {@link #ID_ADMIN} debe
 * mantenerse idéntico al de donaciones-service: es el dato compartido que permite que ambos
 * servicios siembren la misma persona administradora de forma independiente al arrancar, sin
 * depender de la sincronización asincrónica entre servicios.
 */
public final class AdminConstantes {

  private AdminConstantes() {
    throw new IllegalStateException("Utility class");
  }

  public static final UUID ID_ADMIN = UUID.fromString("00000000-0000-0000-0000-000000000001");
  public static final String DENOMINACION_ADMIN = "Administradora Sistema";
  public static final String EMAIL_ADMIN = "admin@donatrack.com";
}
