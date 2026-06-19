package grupo5.donaciones.dto.comunicaciones;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tipo")
@JsonSubTypes({
  @JsonSubTypes.Type(value = EventoDonanteRegistradoDTO.class, name = "DONANTE_REGISTRADO"),
  @JsonSubTypes.Type(value = EventoDonacionAsignadaDTO.class, name = "DONACION_ASIGNADA"),
  @JsonSubTypes.Type(value = EventoDonacionRecibidaDTO.class, name = "DONACION_RECIBIDA")
})
public interface EventoNotificableDTO {
  UUID idPersonaDonante();

  LocalDateTime fecha();
}
