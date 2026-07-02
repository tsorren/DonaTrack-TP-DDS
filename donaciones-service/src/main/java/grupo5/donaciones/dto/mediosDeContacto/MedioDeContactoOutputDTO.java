package grupo5.donaciones.dto.mediosDeContacto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tipo")
@JsonSubTypes({
  @JsonSubTypes.Type(value = CorreoOutputDTO.class, name = "CORREO"),
  @JsonSubTypes.Type(value = TelefonoOutputDTO.class, name = "TELEFONO"),
  @JsonSubTypes.Type(value = WhatsAppOutputDTO.class, name = "WHATSAPP")
})
public sealed interface MedioDeContactoOutputDTO
    permits CorreoOutputDTO, TelefonoOutputDTO, WhatsAppOutputDTO {
  Boolean esPredeterminado();
}
