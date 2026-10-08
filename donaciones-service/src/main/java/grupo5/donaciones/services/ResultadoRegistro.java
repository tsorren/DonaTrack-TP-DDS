package grupo5.donaciones.services;

/**
 * Resultado de un registro idempotente: el recurso resultante y si fue creado ahora ({@code creado
 * = true}) o ya existía ({@code false}). El controller lo traduce a 201 o 200.
 */
public record ResultadoRegistro<T>(T recurso, boolean creado) {}
