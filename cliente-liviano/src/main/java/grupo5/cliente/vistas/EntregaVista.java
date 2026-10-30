package grupo5.cliente.vistas;

/** Una entrega tal como la muestran la tarjeta y la tabla, con los textos ya formateados. */
public record EntregaVista(
    String titulo,
    String destino,
    String fecha,
    EstadoEntrega estado,
    String foto,
    String fotoAlt,
    String enlace) {}
