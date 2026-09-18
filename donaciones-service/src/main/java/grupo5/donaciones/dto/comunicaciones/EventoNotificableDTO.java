package grupo5.donaciones.dto.comunicaciones;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tipo")
@JsonSubTypes({
  @JsonSubTypes.Type(value = EventoDonanteRegistradoDTO.class, name = "DONANTE_REGISTRADO"),
  @JsonSubTypes.Type(value = EventoDonacionAsignadaDTO.class, name = "DONACION_ASIGNADA"),
  @JsonSubTypes.Type(value = EventoDonacionRecibidaDTO.class, name = "DONACION_RECIBIDA"),
  @JsonSubTypes.Type(value = EventoEntregaFallidaDTO.class, name = "ENTREGA_FALLIDA"),
  @JsonSubTypes.Type(value = EventoRutaIniciadaDTO.class, name = "DONACION_EN_CAMINO"),
  @JsonSubTypes.Type(value = EventoDonacionVencidaDTO.class, name = "DONACION_VENCIDA")
})
public interface EventoNotificableDTO {
  UUID idPersonaDonante();

  LocalDateTime fecha();
}
