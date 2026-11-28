package grupo5.donaciones.graphql.donaciones;

import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;

/** El tipo {@code CambioEstado} del esquema: sin quién hizo el cambio ni por qué. */
public record CambioEstadoGraphQl(
    TipoEstadoDonacion estadoAnterior, TipoEstadoDonacion estadoNuevo, String fecha) {}
