package grupo5.notificaciones.dto;

import jakarta.validation.constraints.NotBlank;

// Nota (Oleada 9, RF-09): direccionCorreo/caracteristica/codigoArea/numero quedan sin @NotBlank a
// propósito — son condicionales según `tipo` (un CORREO no tiene caracteristica/codigoArea/numero,
// un TELEFONO/WHATSAPP no tiene direccionCorreo). Bean Validation por campo no expresa "requerido
// si tipo == X" sin un validador cruzado a medida, que no se pidió en esta oleada; esa validación
// de formato específico por tipo ya la resuelve MedioDeContactoMapper (RF-05, Oleada 3).
// esPredeterminado también queda sin @NotNull: null es un valor válido y manejado explícitamente
// (Oleada 1) — MedioDeContactoMapper lo trata como "no predeterminado".
public record MedioDeContactoReplicaDTO(
    @NotBlank(message = "El tipo de medio de contacto es obligatorio") String tipo,
    Boolean esPredeterminado,
    String direccionCorreo,
    String caracteristica,
    String codigoArea,
    String numero) {}
