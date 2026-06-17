package grupo5.donaciones.dto.donacionesIndependientes;

public record CambioEstadoDonacionIndependienteRequestDTO(
    String estado,
    String justificacion, // solo para ENTREGA_FALLIDA
    Long necesidadId // solo para ASIGNADA
    ) {}
