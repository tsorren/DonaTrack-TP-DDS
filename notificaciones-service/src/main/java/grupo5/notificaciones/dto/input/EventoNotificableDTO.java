package grupo5.notificaciones.dto.input;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tipo")
@JsonSubTypes({
  @JsonSubTypes.Type(value = EventoDonanteRegistradoDTO.class, name = "DONANTE_REGISTRADO"),
  @JsonSubTypes.Type(value = EventoDonanteInactivoDTO.class, name = "DONANTE_INACTIVO"),
  @JsonSubTypes.Type(value = EventoMisionCumplidaDTO.class, name = "MISION_CUMPLIDA"),
  @JsonSubTypes.Type(value = EventoSubioCategoriaDTO.class, name = "SUBIO_CATEGORIA"),
  @JsonSubTypes.Type(value = EventoDonacionAsignadaDTO.class, name = "DONACION_ASIGNADA"),
  @JsonSubTypes.Type(value = EventoDonacionRecibidaDTO.class, name = "DONACION_RECIBIDA"),
  @JsonSubTypes.Type(value = EventoDonacionEnCaminoDTO.class, name = "DONACION_EN_CAMINO"),
  @JsonSubTypes.Type(value = EventoEntregaFallidaDTO.class, name = "ENTREGA_FALLIDA")
})
public sealed interface EventoNotificableDTO
    permits EventoDonacionAsignadaDTO,
        EventoDonacionRecibidaDTO,
        EventoDonanteInactivoDTO,
        EventoDonanteRegistradoDTO,
        EventoMisionCumplidaDTO,
        EventoSubioCategoriaDTO,
        EventoDonacionEnCaminoDTO,
        EventoEntregaFallidaDTO {
  UUID idPersonaDonante();

  LocalDateTime fecha();
}
