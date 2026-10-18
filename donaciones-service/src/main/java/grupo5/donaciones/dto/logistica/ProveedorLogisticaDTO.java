package grupo5.donaciones.dto.logistica;

/**
 * Un proveedor de logística configurado. {@code disponible} indica si tiene adapter registrado (si
 * no, sus pedidos se tratan como rechazados y se prueba con el siguiente).
 */
public record ProveedorLogisticaDTO(
    String id, String transporte, boolean disponible, boolean preferido) {}
