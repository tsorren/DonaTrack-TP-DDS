package grupo5.donaciones.dto.personas;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoOutputDTO;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.List;
import java.util.UUID;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "tipo",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = HumanaOutputDTO.class, name = "HUMANA"),
  @JsonSubTypes.Type(value = JuridicaOutputDTO.class, name = "JURIDICA")
})
public sealed interface PersonaOutputDTO permits HumanaOutputDTO, JuridicaOutputDTO {
  UUID id();

  TipoPersona tipo();

  TipoDocumento tipoDocumento();

  String documento();

  DireccionOutputDTO direccion();

  List<MedioDeContactoOutputDTO> mediosDeContacto();
}
