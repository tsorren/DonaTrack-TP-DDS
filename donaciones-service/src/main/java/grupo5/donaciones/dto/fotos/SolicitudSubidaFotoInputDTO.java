package grupo5.donaciones.dto.fotos;

import jakarta.validation.constraints.NotBlank;

/** Pedido de una URL para subir la foto de un bien. */
public record SolicitudSubidaFotoInputDTO(
    @NotBlank(message = "El tipo de contenido de la foto es obligatorio") String tipoContenido) {}
