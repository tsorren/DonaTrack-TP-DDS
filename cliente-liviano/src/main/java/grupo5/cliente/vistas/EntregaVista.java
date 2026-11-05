package grupo5.cliente.vistas;

/**
 * Una entrega tal como la muestran la tarjeta y la tabla, con los textos ya formateados. {@code
 * categoria} es el ícono de lo donado (fragmentos/icono), para la portada cuando no hay foto.
 */
public record EntregaVista(
    String titulo,
    String destino,
    String fecha,
    EstadoEntrega estado,
    String categoria,
    String foto,
    String fotoAlt,
    String enlace) {}
