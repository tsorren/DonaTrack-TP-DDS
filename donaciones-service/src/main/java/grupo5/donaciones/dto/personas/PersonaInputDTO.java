package grupo5.donaciones.dto.personas;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoInputDTO;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "tipo",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = HumanaInputDTO.class, name = "HUMANA"),
  @JsonSubTypes.Type(value = JuridicaInputDTO.class, name = "JURIDICA")
})
public sealed interface PersonaInputDTO permits HumanaInputDTO, JuridicaInputDTO {
  TipoPersona tipo();

  TipoDocumento tipoDocumento();

  String documento();

  DireccionInputDTO direccion();

  List<MedioDeContactoInputDTO> mediosDeContacto();
}
