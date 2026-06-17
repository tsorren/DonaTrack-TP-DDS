package grupo5.donaciones.dto.personas;

import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoOutputDTO;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.List;
import java.util.UUID;

public record JuridicaOutputDTO(
    UUID id,
    TipoPersona tipo,
    TipoDocumento tipoDocumento,
    String documento,
    DireccionOutputDTO direccion,
    List<MedioDeContactoOutputDTO> mediosDeContacto,
    String razonSocial,
    TipoJuridico tipoJuridico,
    String rubro,
    List<HumanaOutputDTO> representantes)
    implements PersonaOutputDTO {}
