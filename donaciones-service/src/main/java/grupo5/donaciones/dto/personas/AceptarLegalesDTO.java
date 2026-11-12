package grupo5.donaciones.dto.personas;

import jakarta.validation.constraints.NotNull;

/** La versión de los legales que la persona acepta: tiene que ser la vigente (HB-20). */
public record AceptarLegalesDTO(@NotNull(message = "La versión es obligatoria") Integer version) {}
