package grupo5.donaciones.dto.personas;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoInputDTO;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record JuridicaInputDTO(
    @NotNull(message = "El tipo de persona es obligatorio") TipoPersona tipo,
    TipoDocumento tipoDocumento,
    String documento,
    @Valid DireccionInputDTO direccion,
    List<@Valid MedioDeContactoInputDTO> mediosDeContacto,
    @NotBlank(message = "La razón social es obligatoria") String razonSocial,
    @NotNull(message = "El tipo jurídico es obligatorio") TipoJuridico tipoJuridico,
    String rubro,
    List<@Valid HumanaInputDTO> representantes)
    implements PersonaInputDTO {}
