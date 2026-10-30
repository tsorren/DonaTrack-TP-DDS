package grupo5.cliente.vistas;

/** Una sección del menú; {@code actual} marca la pantalla en la que está el usuario. */
public record OpcionNavegacion(String texto, String ruta, boolean actual) {}
