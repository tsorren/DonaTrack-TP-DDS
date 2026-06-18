package grupo5.donaciones.dto.donacionesIndependientes;

import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;

public record CambioEstadoDonacionIndependienteRequestDTO(
    TipoEstadoDonacion estado,
    String justificacion, // solo para ENTREGA_FALLIDA
    Long necesidadId // solo para ASIGNADA
    ) {}
