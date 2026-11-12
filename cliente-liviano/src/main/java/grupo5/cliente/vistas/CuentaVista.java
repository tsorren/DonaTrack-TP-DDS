package grupo5.cliente.vistas;

/**
 * La persona que usa el área privada, tal como la muestra el menú de la cuenta. {@code
 * cambiarClave} es la dirección donde cambia su contraseña; nula si el sitio no la ofrece.
 */
public record CuentaVista(String nombre, String rol, String cambiarClave) {

  public CuentaVista(String nombre, String rol) {
    this(nombre, rol, null);
  }

  /** Primera letra del nombre, para el botón de la cuenta. */
  public String inicial() {
    return nombre.substring(0, 1);
  }
}
