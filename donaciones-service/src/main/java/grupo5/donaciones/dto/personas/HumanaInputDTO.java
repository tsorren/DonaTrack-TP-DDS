package grupo5.donaciones.dto.personas;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoInputDTO;
import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record HumanaInputDTO(
    @NotNull(message = "El tipo de persona es obligatorio") TipoPersona tipo,
    TipoDocumento tipoDocumento,
    String documento,
    @Valid DireccionInputDTO direccion,
    List<@Valid MedioDeContactoInputDTO> mediosDeContacto,
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    @NotBlank(message = "El apellido es obligatorio") String apellido,
    Genero genero,
    LocalDate fechaNacimiento)
    implements PersonaInputDTO {}
