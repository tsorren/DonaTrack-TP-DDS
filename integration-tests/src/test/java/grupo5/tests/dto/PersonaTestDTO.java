package grupo5.tests.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonaTestDTO(
    String tipo,
    String tipoDocumento,
    String documento,
    String nombre,
    String apellido,
    String genero,
    String fechaNacimiento,
    String razonSocial,
    String tipoJuridico,
    String rubro,
    List<PersonaTestDTO> representantes,
    List<MedioContactoTestDTO> mediosDeContacto,
    DireccionTestDTO direccion) {}
