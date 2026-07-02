package grupo5.donaciones.dto.donacionesIndependientes;

import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.util.UUID;

public record CambioEstadoDonacionIndependienteRequestDTO(
    TipoEstadoDonacion estado,
    String justificacion, // solo para ENTREGA_FALLIDA
    UUID necesidadId, // solo para ASIGNADA
    String urlMapa, // solo para EN_TRASLADO
    String patenteCamion, // solo para ENTREGADA
    Boolean replanificable // solo para ENTREGA_FALLIDA
    ) {}
