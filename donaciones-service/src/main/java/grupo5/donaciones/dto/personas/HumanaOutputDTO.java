package grupo5.donaciones.dto.personas;

import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoOutputDTO;
import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HumanaOutputDTO(
    TipoPersona tipo,
    UUID id,
    TipoDocumento tipoDocumento,
    String documento,
    DireccionOutputDTO direccion,
    List<MedioDeContactoOutputDTO> mediosDeContacto,
    String nombre,
    String apellido,
    Genero genero,
    LocalDate fechaNacimiento)
    implements PersonaOutputDTO {}
