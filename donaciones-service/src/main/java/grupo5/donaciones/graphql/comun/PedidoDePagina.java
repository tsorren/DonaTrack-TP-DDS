package grupo5.donaciones.graphql.comun;

/**
 * Página y tamaño de una consulta raíz ya validados contra el tope (SPEC-11, §4.4 del contrato).
 *
 * @param pagina desde 0
 * @param tamanio de 1 al tope
 */
public record PedidoDePagina(int pagina, int tamanio) {}
