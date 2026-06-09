package grupo5.common.donaciones.donantes.commands;

public record ModificarPersonaHumanaDTO(
    String nombre,
    String apellido,
    String genero, // "HOMBRE", "MUJER", "NO_BINARIO", "PREFIERO_NO_DECIR"
    String direccion,
    String email,
    String telefono) {}
