package grupo5.cliente.vistas;

import java.util.List;

/**
 * Una cifra de impacto con los textos ya formateados. {@code cambio} y {@code cambioTexto} dicen
 * cómo se movió respecto del período anterior (pueden ser null); {@code barras} son las alturas del
 * gráfico chico, de 0 a 84, con el período actual al final (puede ser null).
 */
public record CifraVista(
    String etiqueta,
    String valor,
    String cambio,
    String cambioTexto,
    List<Integer> barras,
    String ruta) {}
