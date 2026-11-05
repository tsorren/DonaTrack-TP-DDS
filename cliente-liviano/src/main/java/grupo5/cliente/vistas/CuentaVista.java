package grupo5.cliente.vistas;

/** La persona que usa el área privada, tal como la muestra el menú de la cuenta. */
public record CuentaVista(String nombre, String rol) {

  /** Primera letra del nombre, para el botón de la cuenta. */
  public String inicial() {
    return nombre.substring(0, 1);
  }
}
