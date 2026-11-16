package grupo5.cliente.vistas;

/**
 * La salida a lo propio del encabezado público con la sesión iniciada (docs/interfaz/flujos/
 * r11-p1-portada.md): {@code ruta} es el inicio del área ("Ir a mi inicio") o, si {@code
 * pendiente}, la pantalla de la solicitud que espera la aprobación ("Ver mi solicitud").
 */
public record SalidaDeLaCuenta(String ruta, boolean pendiente) {}
