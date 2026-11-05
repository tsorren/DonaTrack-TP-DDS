package grupo5.cliente.vistas;

/**
 * Una sección del menú. {@code textoCorto} es el nombre de la barra inferior del celular; {@code
 * icono}, el de fragmentos/icono; {@code actual} marca la pantalla en la que está el usuario.
 */
public record OpcionNavegacion(
    String texto, String textoCorto, String icono, String ruta, boolean actual) {}
