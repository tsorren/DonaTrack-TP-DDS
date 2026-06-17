package grupo5.donaciones.dto.personas;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoInputDTO;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.List;

public record JuridicaInputDTO(
    TipoPersona tipo,
    TipoDocumento tipoDocumento,
    String documento,
    DireccionInputDTO direccion,
    List<MedioDeContactoInputDTO> mediosDeContacto,
    String razonSocial,
    TipoJuridico tipoJuridico,
    String rubro,
    List<HumanaInputDTO> representantes)
    implements PersonaInputDTO {}
