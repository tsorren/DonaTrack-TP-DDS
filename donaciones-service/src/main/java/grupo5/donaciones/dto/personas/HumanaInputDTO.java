package grupo5.donaciones.dto.personas;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoInputDTO;
import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.time.LocalDate;
import java.util.List;

public record HumanaInputDTO(
    TipoPersona tipo,
    TipoDocumento tipoDocumento,
    String documento,
    DireccionInputDTO direccion,
    List<MedioDeContactoInputDTO> mediosDeContacto,
    String nombre,
    String apellido,
    Genero genero,
    LocalDate fechaNacimiento)
    implements PersonaInputDTO {}
