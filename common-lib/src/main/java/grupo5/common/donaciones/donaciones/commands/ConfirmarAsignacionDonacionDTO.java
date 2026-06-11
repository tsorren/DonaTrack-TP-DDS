package grupo5.common.donaciones.donaciones.commands;

import java.util.UUID;

public record ConfirmarAsignacionDonacionDTO(
    UUID donacionId, UUID entidadId, String mensajeNotificacion) {}
