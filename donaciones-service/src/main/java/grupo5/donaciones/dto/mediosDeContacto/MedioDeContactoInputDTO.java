package grupo5.donaciones.dto.mediosDeContacto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tipo")
@JsonSubTypes({
  @JsonSubTypes.Type(value = CorreoInputDTO.class, name = "CORREO"),
  @JsonSubTypes.Type(value = TelefonoInputDTO.class, name = "TELEFONO"),
  @JsonSubTypes.Type(value = WhatsAppInputDTO.class, name = "WHATSAPP")
})
public sealed interface MedioDeContactoInputDTO
    permits CorreoInputDTO, TelefonoInputDTO, WhatsAppInputDTO {
  Boolean esPredeterminado();
}
