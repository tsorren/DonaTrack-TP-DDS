package grupo5.donaciones.dto.donantes;

import jakarta.validation.constraints.NotNull;

/** Preferencias del donante (HB-20): si la entidad que recibe sus donaciones ve su nombre. */
public record PreferenciasDonanteDTO(
    @NotNull(message = "mostrarNombre es obligatorio") Boolean mostrarNombre) {}
