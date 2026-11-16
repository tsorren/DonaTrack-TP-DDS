package grupo5.cliente.vistas;

/**
 * Una entrega que espera la confirmación de la entidad (RF-12), para Confirmar recepción, Llegó con
 * un problema y Recepción confirmada. {@code clave} la nombra en la dirección ({@code ?entrega=});
 * {@code nombre}, en minúsculas ("lentejas secas"); {@code titulo}, con su cantidad ("Lentejas
 * secas, 15 kg"); {@code salio} y {@code llego}, cuándo ("Hoy, 14:30"); {@code llegoFrase} y {@code
 * llegaron}, lo mismo en una oración; {@code categoria}, el ícono del sprite; {@code pedido}, la
 * necesidad que cubre, y {@code recibido} y {@code pedidoDespues}, cómo va; {@code dono}, el nombre
 * visible de quien donó, o null si lo ocultó, con {@code donoCategoria} (su categoría del ranking)
 * y {@code donoTipo} ({@code empresa} o null); {@code salida}, la cantidad que salió, en {@code
 * unidad}, con {@code decimales} si se puede escribir con coma. {@code remito}, {@code foto},
 * {@code confirmo}, {@code llegoMenos} y {@code detalle} son lo que la maqueta muestra como ya
 * cargado. Lo que no se sabe, null.
 */
public record EntidadEntregaVista(
    String clave,
    String nombre,
    String titulo,
    String cantidad,
    String salio,
    String llego,
    String llegoFrase,
    String llegaron,
    String remito,
    String categoria,
    String categoriaTexto,
    String pedido,
    String recibido,
    String pedidoDespues,
    String dono,
    String donoCategoria,
    String donoTipo,
    String foto,
    String confirmo,
    Integer salida,
    String unidad,
    boolean decimales,
    String llegoMenos,
    String detalle) {}
