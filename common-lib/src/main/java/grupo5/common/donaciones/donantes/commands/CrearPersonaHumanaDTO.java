package grupo5.common.donaciones.donantes.commands;

import java.time.LocalDate;

public record CrearPersonaHumanaDTO(
    String nombre,
    String apellido,
    LocalDate fechaNacimiento,
    String genero, // "HOMBRE", "MUJER", "NO_BINARIO", "PREFIERO_NO_DECIR"
    String tipoDocumento, // "DNI", "LC", "LE", "PASAPORTE"
    String nroDocumento,
    String direccion,
    String email,
    String telefono) {}
